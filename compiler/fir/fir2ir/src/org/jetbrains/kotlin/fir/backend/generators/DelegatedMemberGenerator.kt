/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.delegatedWrapperData
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmNames.JVM_DEFAULT_CLASS_ID
import org.jetbrains.kotlin.name.Name

/**
 * A generator for delegated members from implementation by delegation.
 *
 * It assumes a synthetic field with the super-interface type has been created for the delegate expression. It looks for delegatable
 * methods and properties in the super-interface, and creates corresponding members in the subclass.
 * TODO: generic super interface types and generic delegated members.
 */
class DelegatedMemberGenerator(
    private val components: Fir2IrComponents
) : Fir2IrComponents by components {

    private val baseFunctionSymbols = mutableMapOf<IrFunction, List<FirNamedFunctionSymbol>>()
    private val basePropertySymbols = mutableMapOf<IrProperty, List<FirPropertySymbol>>()

    private data class DeclarationBodyInfo(
        val declaration: IrDeclaration,
        val field: IrField,
        val delegateToSymbol: FirCallableSymbol<*>,
        val delegateToLookupTag: ConeClassLikeLookupTag?
    )

    private val bodiesInfo = mutableListOf<DeclarationBodyInfo>()

    fun generateBodies() {
        for ((declaration, irField, delegateToSymbol, delegateToLookupTag) in bodiesInfo) {
            val callTypeCanBeNullable = Fir2IrImplicitCastInserter.typeCanBeEnhancedOrFlexibleNullable(delegateToSymbol.fir.returnTypeRef)
            when (declaration) {
                is IrSimpleFunction -> {
                    val member = declarationStorage.getIrFunctionSymbol(
                        delegateToSymbol as FirNamedFunctionSymbol, delegateToLookupTag
                    ).owner as? IrSimpleFunction ?: continue
                    val body = createDelegateBody(irField, declaration, member, callTypeCanBeNullable)
                    declaration.body = body
                }
                is IrProperty -> {
                    val member = declarationStorage.getIrPropertySymbol(
                        delegateToSymbol as FirPropertySymbol, delegateToLookupTag
                    ).owner as? IrProperty ?: continue
                    val getter = declaration.getter!!
                    getter.body = createDelegateBody(irField, getter, member.getter!!, callTypeCanBeNullable)
                    if (declaration.isVar) {
                        val setter = declaration.setter!!
                        setter.body = createDelegateBody(irField, setter, member.setter!!, false)
                    }
                }
            }
        }
        bodiesInfo.clear()
    }

    private fun FirClassifierSymbol<*>?.boundClass(): FirClass {
        return when (this) {
            is FirRegularClassSymbol -> fir
            is FirAnonymousObjectSymbol -> fir
            is FirTypeParameterSymbol ->
                fir.bounds.first().coneType.fullyExpandedType(session).lowerBoundIfFlexible().toSymbol(session).boundClass()
            is FirTypeAliasSymbol, null -> throw AssertionError(this?.fir?.render())
        }
    }

    // Generate delegated members for [subClass]. The synthetic field [irField] has the super interface type.
    fun generate(irField: IrField, firField: FirField, firSubClass: FirClass, subClass: IrClass) {
        val subClassLookupTag = firSubClass.symbol.toLookupTag()

        val subClassScope = firSubClass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = false)
        val delegateToType = firField.initializer!!.typeRef.coneType.fullyExpandedType(session).lowerBoundIfFlexible()
        val delegateToClass = delegateToType.toSymbol(session).boundClass()

        val delegateToScope = delegateToClass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = false)
        val delegateToLookupTag = (delegateToType as? ConeClassLikeType)?.lookupTag

        subClassScope.processAllFunctions { functionSymbol ->
            val unwrapped =
                functionSymbol.unwrapDelegateTarget(subClassLookupTag, firField)
                    ?: return@processAllFunctions

            val delegateToSymbol = findDelegateToSymbol(
                unwrapped.symbol,
                delegateToScope::processFunctionsByName,
                delegateToScope::processOverriddenFunctions
            ) ?: return@processAllFunctions

            val irSubFunction = generateDelegatedFunction(
                subClass, firSubClass, functionSymbol.fir
            )
            bodiesInfo += DeclarationBodyInfo(irSubFunction, irField, delegateToSymbol, delegateToLookupTag)
            declarationStorage.cacheDelegationFunction(functionSymbol.fir, irSubFunction)
            subClass.addMember(irSubFunction)
        }

        subClassScope.processAllProperties { propertySymbol ->
            if (propertySymbol !is FirPropertySymbol) return@processAllProperties

            val unwrapped =
                propertySymbol.unwrapDelegateTarget(subClassLookupTag, firField)
                    ?: return@processAllProperties

            val delegateToSymbol = findDelegateToSymbol(
                unwrapped.symbol,
                { name, processor ->
                    delegateToScope.processPropertiesByName(name) {
                        if (it !is FirPropertySymbol) return@processPropertiesByName
                        processor(it)
                    }
                },
                delegateToScope::processOverriddenProperties
            ) ?: return@processAllProperties

            val irSubProperty = generateDelegatedProperty(
                subClass, firSubClass, propertySymbol.fir
            )
            bodiesInfo += DeclarationBodyInfo(irSubProperty, irField, delegateToSymbol, delegateToLookupTag)
            declarationStorage.cacheDelegatedProperty(propertySymbol.fir, irSubProperty)
            subClass.addMember(irSubProperty)
        }
    }

    private inline fun <reified S : FirCallableSymbol<*>> findDelegateToSymbol(
        unwrappedSymbol: S,
        processCallables: (name: Name, processor: (S) -> Unit) -> Unit,
        crossinline processOverridden: (base: S, processor: (S) -> ProcessorAction) -> ProcessorAction
    ): S? {
        var result: S? = null
        // The purpose of this code is to find member in delegate-to scope
        // which matches or overrides unwrappedSymbol (which is in turn taken from subclass scope).
        processCallables(unwrappedSymbol.name) { candidateSymbol ->
            if (result != null) return@processCallables
            if (candidateSymbol === unwrappedSymbol) {
                result = candidateSymbol
                return@processCallables
            }
            processOverridden(candidateSymbol) {
                if (it === unwrappedSymbol) {
                    result = candidateSymbol
                    ProcessorAction.STOP
                } else {
                    ProcessorAction.NEXT
                }
            }
        }
        return result
    }

    fun bindDelegatedMembersOverriddenSymbols(irClass: IrClass) {
        val superClasses by lazy(LazyThreadSafetyMode.NONE) {
            irClass.superTypes.mapNotNullTo(mutableSetOf()) { it.classifierOrNull?.owner as? IrClass }
        }
        for (declaration in irClass.declarations) {
            if (declaration.origin != IrDeclarationOrigin.DELEGATED_MEMBER) continue
            when (declaration) {
                is IrSimpleFunction -> {
                    declaration.overriddenSymbols = baseFunctionSymbols[declaration]?.flatMap {
                        fakeOverrideGenerator.getOverriddenSymbolsInSupertypes(it, superClasses)
                    }?.filter { it.owner != declaration }.orEmpty()
                }
                is IrProperty -> {
                    declaration.overriddenSymbols = basePropertySymbols[declaration]?.flatMap {
                        fakeOverrideGenerator.getOverriddenSymbolsInSupertypes(it, superClasses)
                    }?.filter { it.owner != declaration }.orEmpty()
                }
                else -> continue
            }
        }
    }

    private fun generateDelegatedFunction(
        subClass: IrClass,
        firSubClass: FirClass,
        delegateOverride: FirSimpleFunction
    ): IrSimpleFunction {
        val delegateFunction =
            declarationStorage.createIrFunction(
                delegateOverride, subClass, predefinedOrigin = IrDeclarationOrigin.DELEGATED_MEMBER,
                containingClass = firSubClass.symbol.toLookupTag()
            )
        val baseSymbols = mutableListOf<FirNamedFunctionSymbol>()
        // the overridden symbols should be collected only after all fake overrides for all superclases are created and bound to their
        // overridden symbols, otherwise in some cases they will be left in inconsistent state leading to the errors in IR
        delegateOverride.processOverriddenFunctionSymbols(firSubClass) {
            baseSymbols.add(it)
        }
        baseFunctionSymbols[delegateFunction] = baseSymbols
        annotationGenerator.generate(delegateFunction, delegateOverride)

        return delegateFunction
    }

    private fun createDelegateBody(
        irField: IrField,
        delegateFunction: IrSimpleFunction,
        superFunction: IrSimpleFunction,
        callTypeCanBeNullable: Boolean
    ): IrBlockBody {
        val startOffset = irField.startOffset
        val endOffset = irField.endOffset
        val body = irFactory.createBlockBody(startOffset, endOffset)
        val irCall = IrCallImpl(
            startOffset,
            endOffset,
            delegateFunction.returnType,
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
            superFunction.typeParameters.forEach {
                putTypeArgument(
                    it.index, IrSimpleTypeImpl(
                        delegateFunction.typeParameters[it.index].symbol,
                        hasQuestionMark = false,
                        arguments = emptyList(),
                        annotations = emptyList()
                    )
                )
            }
        }
        val resultType = delegateFunction.returnType
        val irCastOrCall =
            if (callTypeCanBeNullable && !resultType.isNullable()) Fir2IrImplicitCastInserter.implicitNotNullCast(irCall)
            else irCall
        if (superFunction.returnType.isUnit() || superFunction.returnType.isNothing()) {
            body.statements.add(irCastOrCall)
        } else {
            val irReturn = IrReturnImpl(startOffset, endOffset, irBuiltIns.nothingType, delegateFunction.symbol, irCastOrCall)
            body.statements.add(irReturn)
        }
        return body
    }

    private fun generateDelegatedProperty(
        subClass: IrClass,
        firSubClass: FirClass,
        firDelegateProperty: FirProperty
    ): IrProperty {
        val delegateProperty =
            declarationStorage.createIrProperty(
                firDelegateProperty, subClass, predefinedOrigin = IrDeclarationOrigin.DELEGATED_MEMBER,
                containingClass = firSubClass.symbol.toLookupTag()
            )
        // the overridden symbols should be collected only after all fake overrides for all superclases are created and bound to their
        // overridden symbols, otherwise in some cases they will be left in inconsistent state leading to the errors in IR
        val baseSymbols = mutableListOf<FirPropertySymbol>()
        firDelegateProperty.processOverriddenPropertySymbols(firSubClass) {
            baseSymbols.add(it)
        }
        basePropertySymbols[delegateProperty] = baseSymbols
        annotationGenerator.generate(delegateProperty, firDelegateProperty)

        val getter = delegateProperty.getter!!
        getter.overriddenSymbols =
            firDelegateProperty.generateOverriddenAccessorSymbols(
                firSubClass,
                isGetter = true
            )
        annotationGenerator.generate(getter, firDelegateProperty)
        if (delegateProperty.isVar) {
            val setter = delegateProperty.setter!!
            setter.overriddenSymbols =
                firDelegateProperty.generateOverriddenAccessorSymbols(
                    firSubClass, isGetter = false
                )
            annotationGenerator.generate(setter, firDelegateProperty)
        }

        return delegateProperty
    }

    companion object {
        private val PLATFORM_DEPENDENT_CLASS_ID = ClassId.topLevel(FqName("kotlin.internal.PlatformDependent"))

        context(Fir2IrComponents)
        private fun <S : FirCallableSymbol<D>, D : FirCallableDeclaration> S.unwrapDelegateTarget(
            subClassLookupTag: ConeClassLikeLookupTag,
            firField: FirField,
        ): D? {
            val callable = this.fir as? D ?: return null

            val delegatedWrapperData = callable.delegatedWrapperData ?: return null
            if (delegatedWrapperData.containingClass != subClassLookupTag) return null
            if (delegatedWrapperData.delegateField != firField) return null

            val wrapped = delegatedWrapperData.wrapped as? D ?: return null

            @Suppress("UNCHECKED_CAST")
            val wrappedSymbol = wrapped.symbol as? S ?: return null

            @Suppress("UNCHECKED_CAST")
            return (wrappedSymbol.unwrapCallRepresentative().fir as D).takeIf { !shouldSkipDelegationFor(it) }
        }

        private fun shouldSkipDelegationFor(unwrapped: FirCallableDeclaration): Boolean {
            // See org.jetbrains.kotlin.resolve.jvm.JvmDelegationFilter
            return (unwrapped is FirSimpleFunction && unwrapped.isDefaultJavaMethod()) ||
                    unwrapped.hasAnnotation(JVM_DEFAULT_CLASS_ID) ||
                    unwrapped.hasAnnotation(PLATFORM_DEPENDENT_CLASS_ID)
        }

        private fun FirSimpleFunction.isDefaultJavaMethod(): Boolean =
            when {
                isIntersectionOverride ->
                    baseForIntersectionOverride!!.isDefaultJavaMethod()
                isSubstitutionOverride ->
                    originalForSubstitutionOverride!!.isDefaultJavaMethod()
                else -> {
                    // Check that we have a non-abstract method from Java interface
                    origin == FirDeclarationOrigin.Enhancement && modality == Modality.OPEN
                }
            }
    }
}

