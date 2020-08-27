/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.backend.common.ir.isFinalClass
import org.jetbrains.kotlin.backend.common.ir.isOverridable
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities as OldVisibilities
import org.jetbrains.kotlin.fir.Visibilities
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.backend.declareThisReceiverParameter
import org.jetbrains.kotlin.fir.backend.isOverriding
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameterCopy
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.WrappedPropertyDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedTypeParameterDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.DFS

/**
 * A generator for delegated members from implementation by delegation.
 *
 * It assumes a synthetic field with the super-interface type has been created for the delegate expression. It looks for delegatable
 * methods and properties in the super-interface, and creates corresponding members in the subclass.
 * TODO: generic super interface types and generic delegated members.
 */
internal class DelegatedMemberGenerator(
    private val components: Fir2IrComponents
) : Fir2IrComponents by components {

    // Generate delegated members for [subClass]. The synthetic field [irField] has the super interface type.
    fun generate(irField: IrField, firSubClass: FirClass<*>, subClass: IrClass) {
        val delegateClass = (irField.type as IrSimpleTypeImpl).classOrNull?.owner ?: return
        val subClasses = mutableMapOf(delegateClass to mutableListOf(subClass))
        DFS.dfs(
            listOf(delegateClass), { node -> node.superTypes.mapNotNull { it.classOrNull?.owner } },
            object : DFS.NodeHandler<IrClass, Unit> {
                override fun beforeChildren(current: IrClass): Boolean {
                    for (superType in current.superTypes) {
                        superType.classOrNull?.owner?.let { subClasses.getOrPut(it) { mutableListOf() }.add(current) }
                    }
                    return true
                }

                override fun afterChildren(current: IrClass) = Unit
                override fun result() = Unit
            }
        )

        for ((superClass, mayOverride) in subClasses) {
            val superClassId = superClass.classId ?: continue
            if (superClassId == irBuiltIns.anyClass.owner.classId) continue
            for (member in superClass.declarations) {
                val delegatable = member.isDelegatable() && !DFS.ifAny(mayOverride, { subClasses[it] ?: emptyList() }) {
                    // Delegate to the most specific version of each member.
                    it.declarations.any { !it.isFakeOverride && it.overrides(member) }
                }
                if (!delegatable) continue
                val scope = firSubClass.unsubstitutedScope(session, scopeSession)
                if (member is IrSimpleFunction) {
                    val firSuperFunction = declarationStorage.findOverriddenFirFunction(member, superClassId) ?: return
                    var firSubFunction: FirSimpleFunction? = null
                    scope.processFunctionsByName(member.name) {
                        if (it.callableId.classId == firSubClass.classId) {
                            var overriddenFunctionSymbol = it.overriddenSymbol
                            while (overriddenFunctionSymbol != null) {
                                if (overriddenFunctionSymbol.fir == firSuperFunction) {
                                    firSubFunction = it.fir as FirSimpleFunction
                                    break
                                }
                                overriddenFunctionSymbol = overriddenFunctionSymbol.overriddenSymbol
                            }
                        }
                    }
                    val irSubFunction = generateDelegatedFunction(subClass, irField, member, firSuperFunction)
                    firSubFunction?.let { declarationStorage.cacheIrSimpleFunction(it, irSubFunction) }
                    subClass.addMember(irSubFunction)
                } else if (member is IrProperty) {
                    val firSuperProperty = declarationStorage.findOverriddenFirProperty(member, superClassId) ?: return
                    var firSubProperty: FirProperty? = null
                    scope.processPropertiesByName(member.name) {
                        if (it.callableId.classId == firSubClass.classId) {
                            var overriddenPropertySymbol = it.overriddenSymbol
                            while (overriddenPropertySymbol != null) {
                                if (overriddenPropertySymbol.fir == firSuperProperty) {
                                    firSubProperty = it.fir as FirProperty
                                    break
                                }
                                overriddenPropertySymbol = overriddenPropertySymbol.overriddenSymbol
                            }
                        }
                    }
                    val irSubProperty = generateDelegatedProperty(subClass, irField, member, firSuperProperty)
                    firSubProperty?.let { declarationStorage.cacheIrProperty(it, irSubProperty) }
                }
            }
        }
    }

    private fun IrDeclaration.overrides(other: IrDeclaration) = when {
        // Imported declarations have overridden symbols, so check that first.
        this is IrProperty && other is IrProperty ->
            getter?.let { getter -> other.getter?.symbol in getter.overriddenSymbols }
                ?: setter?.let { setter -> other.setter?.symbol in setter.overriddenSymbols }
                ?: false
        this is IrSimpleFunction && other is IrSimpleFunction ->
            other.symbol in overriddenSymbols
        else ->
            false
    } || isOverriding(irBuiltIns, this, other) // TODO check if this behaves correctly with generic arguments

    private fun IrDeclaration.isDelegatable(): Boolean {
        return isOverridable()
                && !isFakeOverride
                && !(this is IrSimpleFunction && hasDefaultImplementation())
    }

    private fun IrDeclaration.isOverridable(): Boolean {
        return when (this) {
            is IrSimpleFunction -> this.isOverridable
            is IrProperty -> visibility != OldVisibilities.PRIVATE && modality != Modality.FINAL && (parent as? IrClass)?.isFinalClass != true
            else -> false
        }
    }

    private fun IrSimpleFunction.hasDefaultImplementation(): Boolean {
        var realFunction: IrSimpleFunction? = this
        while (realFunction != null && realFunction.isFakeOverride) {
            realFunction = realFunction.overriddenSymbols.firstOrNull()?.owner
        }
        return realFunction != null
                && (realFunction.modality != Modality.ABSTRACT && realFunction.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
                || realFunction.annotations.hasAnnotation(FqName("kotlin.jvm.JvmDefault")))
    }

    private fun generateDelegatedFunction(
        subClass: IrClass,
        irField: IrField,
        superFunction: IrSimpleFunction,
        firSuperFunction: FirFunction<*>
    ): IrSimpleFunction {
        val startOffset = irField.startOffset
        val endOffset = irField.endOffset
        val descriptor = WrappedSimpleFunctionDescriptor()
        val origin = IrDeclarationOrigin.DELEGATED_MEMBER
        val modality = if (superFunction.modality == Modality.ABSTRACT) Modality.OPEN else superFunction.modality
        // TODO: external classes, as the type parameters are converted using deserialized descriptors when used inside the classes.
        val addTypeSubstitution = irField.type.classOrNull?.owner?.origin?.let {
            it != IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
                    && it != IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
        } == true
        lateinit var irTypeSubstitutor: IrTypeSubstitutor
        val delegateFunction = symbolTable.declareSimpleFunction(descriptor) { symbol ->
            irFactory.createFunction(
                startOffset,
                endOffset,
                origin,
                symbol,
                superFunction.name,
                superFunction.visibility,
                modality,
                superFunction.returnType,
                superFunction.isInline,
                superFunction.isExternal,
                superFunction.isTailrec,
                superFunction.isSuspend,
                superFunction.isOperator,
                superFunction.isInfix,
                superFunction.isExpect
            ).apply {
                descriptor.bind(this)
                declarationStorage.enterScope(this)
                this.parent = subClass
                overriddenSymbols = listOf(superFunction.symbol)
                dispatchReceiverParameter = declareThisReceiverParameter(symbolTable, subClass.defaultType, origin)
                if (addTypeSubstitution) {
                    val substParameters = mutableListOf<IrTypeParameterSymbol>()
                    val substArguments = mutableListOf<IrTypeArgument>()
                    initializeTypeSubstitution(substParameters, substArguments, irField.type)
                    typeParameters = superFunction.typeParameters.map { typeParameter ->
                        val parameterDescriptor = WrappedTypeParameterDescriptor()
                        symbolTable.declareScopedTypeParameter(
                            startOffset, endOffset, origin, parameterDescriptor
                        ) { symbol ->
                            irFactory.createTypeParameter(
                                startOffset,
                                endOffset,
                                origin,
                                symbol,
                                typeParameter.name,
                                typeParameter.index,
                                typeParameter.isReified,
                                typeParameter.variance
                            ).also {
                                parameterDescriptor.bind(it)
                                it.parent = this
                                substParameters.add(typeParameter.symbol)
                                substArguments.add(
                                    makeTypeProjection(
                                        variance = Variance.INVARIANT,
                                        type = IrSimpleTypeImpl(it.symbol, false, emptyList(), emptyList())
                                    )
                                )
                                it.superTypes += typeParameter.superTypes
                            }
                        }
                    }
                    irTypeSubstitutor = IrTypeSubstitutor(substParameters, substArguments, irBuiltIns)
                }
                valueParameters = superFunction.valueParameters.map { valueParameter ->
                    val parameterDescriptor = WrappedValueParameterDescriptor()
                    val substedType = if (addTypeSubstitution) irTypeSubstitutor.substitute(valueParameter.type) else valueParameter.type
                    symbolTable.declareValueParameter(
                        startOffset, endOffset, origin, parameterDescriptor, substedType
                    ) { symbol ->
                        irFactory.createValueParameter(
                            startOffset, endOffset, origin, symbol,
                            valueParameter.name, valueParameter.index, substedType,
                            null, valueParameter.isCrossinline, valueParameter.isNoinline
                        ).also {
                            parameterDescriptor.bind(it)
                            it.parent = this
                        }
                    }
                }

                val visibility = when (firSuperFunction) {
                    is FirSimpleFunction -> firSuperFunction.status.visibility
                    is FirPropertyAccessor -> firSuperFunction.status.visibility
                    else -> Visibilities.Public
                }
                metadata = FirMetadataSource.Function(
                    buildSimpleFunction {
                        this.origin = FirDeclarationOrigin.Synthetic
                        this.name = superFunction.name
                        this.symbol = FirNamedFunctionSymbol(getCallableId(subClass, superFunction.name))
                        this.status = FirDeclarationStatusImpl(visibility, modality)
                        this.session = components.session
                        this.returnTypeRef = firSuperFunction.returnTypeRef
                        firSuperFunction.valueParameters.map { superParameter ->
                            this.valueParameters.add(
                                buildValueParameterCopy(superParameter) {
                                    this.origin = FirDeclarationOrigin.Synthetic
                                    this.session = components.session
                                    this.symbol = FirVariableSymbol(superParameter.name)
                                }
                            )
                        }
                    }
                )

                declarationStorage.leaveScope(this)
            }
        }

        val body = irFactory.createBlockBody(startOffset, endOffset)
        val irCall = IrCallImpl(
            startOffset,
            endOffset,
            if (addTypeSubstitution) irTypeSubstitutor.substitute(superFunction.returnType) else superFunction.returnType,
            superFunction.symbol,
            superFunction.typeParameters.size,
            superFunction.valueParameters.size
        ).apply {
            dispatchReceiver =
                IrGetFieldImpl(
                    startOffset, endOffset,
                    irField.symbol,
                    irField.type,
                    IrGetValueImpl(
                        startOffset, endOffset,
                        delegateFunction.dispatchReceiverParameter?.type!!,
                        delegateFunction.dispatchReceiverParameter?.symbol!!
                    )
                )
            extensionReceiver =
                delegateFunction.extensionReceiverParameter?.let { extensionReceiver ->
                    IrGetValueImpl(startOffset, endOffset, extensionReceiver.type, extensionReceiver.symbol)
                }
            delegateFunction.valueParameters.forEach {
                putValueArgument(it.index, IrGetValueImpl(startOffset, endOffset, it.type, it.symbol))
            }
        }
        if (superFunction.returnType.isUnit() || superFunction.returnType.isNothing()) {
            body.statements.add(irCall)
        } else {
            val irReturn = IrReturnImpl(startOffset, endOffset, irBuiltIns.nothingType, delegateFunction.symbol, irCall)
            body.statements.add(irReturn)
        }
        delegateFunction.body = body
        return delegateFunction
    }

    private fun initializeTypeSubstitution(
        typeParameters: MutableList<IrTypeParameterSymbol>,
        typeArguments: MutableList<IrTypeArgument>,
        type: IrType
    ) {
        if (type is IrSimpleTypeImpl && type.arguments.isNotEmpty()) {
            val classTypeParameters = type.classOrNull?.owner?.typeParameters
            if (classTypeParameters?.size == type.arguments.size) {
                typeParameters.addAll(classTypeParameters.map { it.symbol })
                typeArguments.addAll(type.arguments)
            }
        }
    }

    private fun generateDelegatedProperty(
        subClass: IrClass,
        irField: IrField,
        superProperty: IrProperty,
        firSuperProperty: FirProperty
    ): IrProperty {
        val startOffset = irField.startOffset
        val endOffset = irField.endOffset
        val descriptor = WrappedPropertyDescriptor()
        val modality = if (superProperty.modality == Modality.ABSTRACT) Modality.OPEN else superProperty.modality
        return symbolTable.declareProperty(
            startOffset, endOffset,
            IrDeclarationOrigin.DELEGATED_MEMBER, descriptor, superProperty.isDelegated
        ) { symbol ->
            irFactory.createProperty(
                startOffset, endOffset, IrDeclarationOrigin.DELEGATED_MEMBER, symbol,
                superProperty.name, superProperty.visibility,
                modality,
                isVar = superProperty.isVar,
                isConst = superProperty.isConst,
                isLateinit = superProperty.isLateinit,
                isDelegated = superProperty.isDelegated,
                isExternal = false,
                isExpect = superProperty.isExpect,
                isFakeOverride = false
            ).apply {
                descriptor.bind(this)
                this.parent = subClass
                getter = generateDelegatedFunction(subClass, irField, superProperty.getter!!, firSuperProperty.getter!!).apply {
                    this.correspondingPropertySymbol = symbol
                }
                if (superProperty.isVar) {
                    setter = generateDelegatedFunction(subClass, irField, superProperty.setter!!, firSuperProperty.setter!!).apply {
                        this.correspondingPropertySymbol = symbol
                    }
                }
                this.metadata = FirMetadataSource.Property(
                    buildProperty {
                        this.name = superProperty.name
                        this.origin = FirDeclarationOrigin.Synthetic
                        this.session = components.session
                        this.status = FirDeclarationStatusImpl(firSuperProperty.status.visibility, modality)
                        this.isLocal = firSuperProperty.isLocal
                        this.returnTypeRef = firSuperProperty.returnTypeRef
                        this.symbol = FirPropertySymbol(getCallableId(subClass, superProperty.name))
                        this.isVar = firSuperProperty.isVar
                    }
                )
                subClass.addMember(this)
            }
        }
    }

    private fun getCallableId(irClass: IrClass, name: Name): CallableId {
        val classId = irClass.classId
        return if (classId != null) CallableId(classId, name) else CallableId(name)
    }
}
