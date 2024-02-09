/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.isLocalClassOrAnonymousObject
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_DEFAULT_CLASS_ID
import org.jetbrains.kotlin.name.Name

/**
 * A generator for delegated members from implementation by delegation.
 *
 * It assumes a synthetic field with the super-interface type has been created for the delegate expression. It looks for delegatable
 * methods and properties in the super-interface, and creates corresponding members in the subclass.
 * TODO: generic super interface types and generic delegated members.
 */
class DelegatedMemberGenerator(private val components: Fir2IrComponents) : Fir2IrComponents by components {
    private val baseFunctionSymbols: MutableMap<IrFunction, Collection<FirNamedFunctionSymbol>> = mutableMapOf()
    private val basePropertySymbols: MutableMap<IrProperty, Collection<FirPropertySymbol>> = mutableMapOf()

    private data class DeclarationBodyInfo(
        val delegatedFirDeclaration: FirCallableDeclaration,
        val delegatedIrDeclaration: IrDeclaration,
        val irField: IrField,
        val delegateToSymbol: FirCallableSymbol<*>,
        val delegateToLookupTag: ConeClassLikeLookupTag?
    )

    private val bodiesInfo = mutableListOf<DeclarationBodyInfo>()

    fun generateBodies() {
        for ((delegatedFirDeclaration, delegatedIrDeclaration, irField, delegateToFirSymbol, delegateToLookupTag) in bodiesInfo) {
            when (delegatedIrDeclaration) {
                is IrSimpleFunction -> {
                    val delegateToIrFunctionSymbol = declarationStorage.getIrFunctionSymbol(
                        delegateToFirSymbol.unwrapCallRepresentative(delegateToLookupTag) as FirNamedFunctionSymbol,
                        delegateToLookupTag
                    ) as? IrSimpleFunctionSymbol ?: continue
                    val body = createDelegateBody(
                        irField, delegatedFirDeclaration, delegatedIrDeclaration, delegateToFirSymbol.fir, delegateToIrFunctionSymbol,
                        isSetter = false
                    )
                    delegatedIrDeclaration.body = body
                }
                is IrProperty -> {
                    val delegateToIrPropertySymbol = declarationStorage.getIrPropertySymbol(
                        delegateToFirSymbol.unwrapCallRepresentative(delegateToLookupTag) as FirPropertySymbol,
                        delegateToLookupTag
                    ) as? IrPropertySymbol ?: continue
                    val delegateToGetterSymbol = declarationStorage.findGetterOfProperty(delegateToIrPropertySymbol)!!
                    val getter = delegatedIrDeclaration.getter!!
                    getter.body = createDelegateBody(
                        irField, delegatedFirDeclaration, getter, delegateToFirSymbol.fir, delegateToGetterSymbol,
                        isSetter = false
                    )
                    if (delegatedIrDeclaration.isVar) {
                        val delegateToSetterSymbol = declarationStorage.findSetterOfProperty(delegateToIrPropertySymbol)!!
                        val setter = delegatedIrDeclaration.setter!!
                        setter.body = createDelegateBody(
                            irField, delegatedFirDeclaration, setter, delegateToFirSymbol.fir, delegateToSetterSymbol,
                            isSetter = true
                        )
                    }
                }
            }
        }
        bodiesInfo.clear()
    }

    fun generateWithBodiesIfNeeded(firField: FirField, irField: IrField, firSubClass: FirClass, subClass: IrClass) {
        delegatedMemberGenerator.generate(irField, firField, firSubClass, subClass)
        if (firSubClass.isLocalClassOrAnonymousObject()) {
            delegatedMemberGenerator.generateBodies()
        }
    }

    // Generate delegated members for [subClass]. The synthetic field [irField] has the super interface type.
    fun generate(irField: IrField, firField: FirField, firSubClass: FirClass, subClass: IrClass) {
        val subClassScope = firSubClass.unsubstitutedScope()

        val delegateToScope = firField.initializer!!.resolvedType
            .fullyExpandedType(session)
            .lowerBoundIfFlexible()
            .scope(session, scopeSession, CallableCopyTypeCalculator.Forced, null) ?: return

        val subClassLookupTag = firSubClass.symbol.toLookupTag()

        subClassScope.processAllFunctions { functionSymbol ->
            val unwrapped =
                functionSymbol.unwrapDelegateTarget(subClassLookupTag, firField)
                    ?: return@processAllFunctions

            val delegateToSymbol = findDelegateToSymbol(
                unwrapped.symbol,
                delegateToScope::processFunctionsByName,
                delegateToScope::processOverriddenFunctions
            ) ?: return@processAllFunctions

            val delegateToLookupTag = delegateToSymbol.dispatchReceiverClassLookupTagOrNull()
                ?: return@processAllFunctions

            val delegatedFunction = functionSymbol.fir
            val irSubFunction = generateDelegatedFunction(subClass, firSubClass, delegatedFunction)
            bodiesInfo += DeclarationBodyInfo(delegatedFunction, irSubFunction, irField, delegateToSymbol, delegateToLookupTag)
            declarationStorage.cacheDelegationFunction(delegatedFunction, irSubFunction)
        }

        subClassScope.processAllProperties { propertySymbol ->
            if (propertySymbol !is FirPropertySymbol) return@processAllProperties

            val unwrapped = propertySymbol.unwrapDelegateTarget(subClassLookupTag, firField) ?: return@processAllProperties

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

            val delegateToLookupTag = delegateToSymbol.dispatchReceiverClassLookupTagOrNull() ?: return@processAllProperties
            val delegatedProperty = propertySymbol.fir
            val irSubProperty = generateDelegatedProperty(subClass, firSubClass, delegatedProperty)
            bodiesInfo += DeclarationBodyInfo(delegatedProperty, irSubProperty, irField, delegateToSymbol, delegateToLookupTag)
            declarationStorage.cacheDelegatedProperty(delegatedProperty, irSubProperty)
        }
    }

    private inline fun <reified S : FirCallableSymbol<*>> findDelegateToSymbol(
        symbol: S,
        processCallables: (name: Name, processor: (S) -> Unit) -> Unit,
        crossinline processOverridden: (base: S, processor: (S) -> ProcessorAction) -> ProcessorAction
    ): S? {
        val unwrappedSymbol = symbol.unwrapUseSiteSubstitutionOverrides()
        var result: S? = null
        /*
         * The purpose of this code is to find member in delegate-to scope
         * which matches or overrides unwrappedSymbol (which is in turn taken from subclass scope).
         *
         * Note that it's important to not unwrap call-site substitution override of the found function, because
         *   later it will be used to compute the proper substituted type for call of this function
         */
        processCallables(unwrappedSymbol.name) { candidateSymbol ->
            if (result != null) return@processCallables
            val unwrappedCandidateSymbol = candidateSymbol.unwrapUseSiteSubstitutionOverrides()
            if (unwrappedCandidateSymbol === unwrappedSymbol) {
                result = candidateSymbol
                return@processCallables
            }
            processOverridden(candidateSymbol) { overriddenSymbol ->
                val unwrappedOverriddenSymbol = overriddenSymbol.unwrapUseSiteSubstitutionOverrides()
                if (unwrappedOverriddenSymbol === unwrappedSymbol) {
                    result = candidateSymbol
                    ProcessorAction.STOP
                } else {
                    ProcessorAction.NEXT
                }
            }
        }
        return result
    }

    @OptIn(FirBasedFakeOverrideGenerator::class) // checked for useIrFakeOverrideBuilder
    fun bindDelegatedMembersOverriddenSymbols(irClass: IrClass) {
        if (components.configuration.useIrFakeOverrideBuilder) return
        val superClasses by lazy(LazyThreadSafetyMode.NONE) {
            irClass.superTypes.mapNotNullTo(mutableSetOf()) {
                // All class symbols should be already bound at this moment
                @OptIn(UnsafeDuringIrConstructionAPI::class)
                it.classifierOrNull?.owner as? IrClass
            }
        }

        require(irClass !is Fir2IrLazyClass)
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        val declarations = irClass.declarations

        for (declaration in declarations) {
            if (declaration.origin != IrDeclarationOrigin.DELEGATED_MEMBER) continue
            when (declaration) {
                is IrSimpleFunction -> {
                    val symbol = declaration.symbol
                    declaration.overriddenSymbols = baseFunctionSymbols[declaration]?.flatMap {
                        fakeOverrideGenerator.getOverriddenSymbolsInSupertypes(it, superClasses)
                    }?.filter { it != symbol }.orEmpty()
                }
                is IrProperty -> {
                    val symbol = declaration.symbol
                    declaration.overriddenSymbols = basePropertySymbols[declaration]?.flatMap {
                        fakeOverrideGenerator.getOverriddenSymbolsInSupertypes(it, superClasses)
                    }?.filter { it != symbol }.orEmpty()
                    declaration.getter!!.overriddenSymbols = declaration.overriddenSymbols.mapNotNull {
                        declarationStorage.findGetterOfProperty(it)
                    }
                    if (declaration.isVar) {
                        declaration.setter!!.overriddenSymbols = declaration.overriddenSymbols.mapNotNull {
                            declarationStorage.findSetterOfProperty(it)
                        }
                    }
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
        val delegateFunction = declarationStorage.createAndCacheIrFunction(
            delegateOverride, subClass, predefinedOrigin = IrDeclarationOrigin.DELEGATED_MEMBER,
            fakeOverrideOwnerLookupTag = firSubClass.symbol.toLookupTag()
        )
        val baseSymbols = mutableSetOf<FirNamedFunctionSymbol>()
        // the overridden symbols should be collected only after all fake overrides for all superclases are created and bound to their
        // overridden symbols, otherwise in some cases they will be left in inconsistent state leading to the errors in IR
        delegateOverride.processOverriddenFunctionSymbols(firSubClass) {
            baseSymbols.add(it)
        }
        baseFunctionSymbols[delegateFunction] = baseSymbols
        annotationGenerator.generate(delegateFunction, delegateOverride)

        return delegateFunction
    }

    /**
     * interface Base {
     *     fun foo(): String
     * }
     *
     * class Impl : Base {
     *     override fun foo(): String {   <-------------- [originalFirFunction], [originalFunctionSymbol]
     *         return "OK"
     *     }
     * }
     *
     * class Delegated(impl: Impl) : Base by impl {
     *     private field delegate_xxx: Impl = impl   <-------------- [irField]
     *     generated override fun foo(): String   <-------------- [delegateFunction]
     * }
     *
     */
    private fun createDelegateBody(
        irField: IrField,
        delegatedFirDeclaration: FirCallableDeclaration,
        delegatedIrFunction: IrSimpleFunction,
        originalFirDeclaration: FirCallableDeclaration,
        originalFunctionSymbol: IrSimpleFunctionSymbol,
        isSetter: Boolean
    ): IrBlockBody {
        val startOffset = SYNTHETIC_OFFSET
        val endOffset = SYNTHETIC_OFFSET
        val body = irFactory.createBlockBody(startOffset, endOffset)

        val typeOrigin = when {
            originalFirDeclaration is FirPropertyAccessor && originalFirDeclaration.isSetter -> ConversionTypeOrigin.SETTER
            else -> ConversionTypeOrigin.DEFAULT
        }

        val callTypeCanBeNullable: Boolean
        val callReturnType = when (isSetter) {
            false -> {
                val substitution = originalFirDeclaration.typeParameters.zip(delegatedFirDeclaration.typeParameters)
                    .map { (original, delegated) ->
                        original.symbol to delegated.symbol.defaultType
                    }.toMap()

                val substitutor = substitutorByMap(substitution, session)
                val substitutedType = substitutor.substituteOrSelf(originalFirDeclaration.returnTypeRef.coneType)
                callTypeCanBeNullable = Fir2IrImplicitCastInserter.typeCanBeEnhancedOrFlexibleNullable(substitutedType, session)
                substitutedType.toIrType(typeOrigin)
            }
            true -> {
                callTypeCanBeNullable = false
                irBuiltIns.unitType
            }
        }

        val irCall = IrCallImpl(
            startOffset,
            endOffset,
            callReturnType,
            originalFunctionSymbol,
            originalFirDeclaration.typeParameters.size,
            originalFirDeclaration.numberOfIrValueParameters(isSetter)
        ).apply {
            val getField = IrGetFieldImpl(
                startOffset, endOffset,
                irField.symbol,
                irField.type,
                IrGetValueImpl(
                    startOffset, endOffset,
                    delegatedIrFunction.dispatchReceiverParameter?.type!!,
                    delegatedIrFunction.dispatchReceiverParameter?.symbol!!
                )
            )

            // When the delegation expression has an intersection type, it is not guaranteed that the field will have the same type as the
            // dispatch receiver of the target method. Therefore, we need to check if a cast must be inserted.
            val superFunctionDispatchReceiverType = originalFirDeclaration.dispatchReceiverType
            val superFunctionDispatchReceiverLookupTag = (superFunctionDispatchReceiverType as? ConeClassLikeType)?.lookupTag
            val superFunctionParentSymbol = superFunctionDispatchReceiverLookupTag?.let { classifierStorage.findIrClass(it)?.symbol }
            dispatchReceiver = if (superFunctionParentSymbol == null || irField.type.isSubtypeOfClass(superFunctionParentSymbol)) {
                getField
            } else {
                Fir2IrImplicitCastInserter.implicitCastOrExpression(getField, superFunctionDispatchReceiverType.toIrType())
            }

            extensionReceiver =
                delegatedIrFunction.extensionReceiverParameter?.let { extensionReceiver ->
                    IrGetValueImpl(startOffset, endOffset, extensionReceiver.type, extensionReceiver.symbol)
                }
            delegatedIrFunction.valueParameters.forEach {
                putValueArgument(it.index, IrGetValueImpl(startOffset, endOffset, it.type, it.symbol))
            }
            for (index in originalFirDeclaration.typeParameters.indices) {
                putTypeArgument(
                    index, IrSimpleTypeImpl(
                        delegatedIrFunction.typeParameters[index].symbol,
                        hasQuestionMark = false,
                        arguments = emptyList(),
                        annotations = emptyList()
                    )
                )
            }
        }
        val resultType = delegatedIrFunction.returnType

        val irCastOrCall =
            if (callTypeCanBeNullable && !resultType.isNullable()) Fir2IrImplicitCastInserter.implicitNotNullCast(irCall)
            else irCall
        val originalDeclarationReturnType = originalFirDeclaration.returnTypeRef.coneType
        if (isSetter || originalDeclarationReturnType.isUnit || originalDeclarationReturnType.isNothing) {
            body.statements.add(irCastOrCall)
        } else {
            val irReturn = IrReturnImpl(startOffset, endOffset, irBuiltIns.nothingType, delegatedIrFunction.symbol, irCastOrCall)
            body.statements.add(irReturn)
        }
        return body
    }

    private fun FirCallableDeclaration.numberOfIrValueParameters(isSetter: Boolean): Int {
        var result = contextReceivers.size
        when {
            this is FirFunction -> result += valueParameters.size
            this is FirProperty && isSetter -> result += 1
        }
        return result
    }

    private fun generateDelegatedProperty(
        subClass: IrClass,
        firSubClass: FirClass,
        firDelegateProperty: FirProperty
    ): IrProperty {
        val delegateProperty = declarationStorage.createAndCacheIrProperty(
            firDelegateProperty, subClass, predefinedOrigin = IrDeclarationOrigin.DELEGATED_MEMBER,
            fakeOverrideOwnerLookupTag = firSubClass.symbol.toLookupTag()
        )
        // the overridden symbols should be collected only after all fake overrides for all superclases are created and bound to their
        // overridden symbols, otherwise in some cases they will be left in inconsistent state leading to the errors in IR
        val baseSymbols = mutableSetOf<FirPropertySymbol>()
        firDelegateProperty.processOverriddenPropertySymbols(firSubClass) {
            baseSymbols.add(it)
        }
        basePropertySymbols[delegateProperty] = baseSymbols
        // Do not generate annotations on property to copy K1 behavior, see KT-57228.
        // But generate annotations on accessors, see KT-64466.
        firDelegateProperty.getter?.let { firGetter ->
            annotationGenerator.generate(delegateProperty.getter!!, firGetter)
        }

        firDelegateProperty.setter?.let { firSetter ->
            annotationGenerator.generate(delegateProperty.setter!!, firSetter)
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
            val callable = this.fir

            val delegatedWrapperData = callable.delegatedWrapperData ?: return null
            if (delegatedWrapperData.containingClass != subClassLookupTag) return null
            if (delegatedWrapperData.delegateField != firField) return null

            val wrapped = delegatedWrapperData.wrapped

            @Suppress("UNCHECKED_CAST")
            val wrappedSymbol = wrapped.symbol as? S ?: return null

            @Suppress("UNCHECKED_CAST")
            return (wrappedSymbol.unwrapCallRepresentative().fir as D).takeIf { !shouldSkipDelegationFor(it, session) }
        }

        private fun shouldSkipDelegationFor(unwrapped: FirCallableDeclaration, session: FirSession): Boolean {
            // See org.jetbrains.kotlin.resolve.jvm.JvmDelegationFilter
            return (unwrapped is FirSimpleFunction && unwrapped.isDefaultJavaMethod()) ||
                    unwrapped.hasAnnotation(JVM_DEFAULT_CLASS_ID, session) ||
                    unwrapped.hasAnnotation(PLATFORM_DEPENDENT_CLASS_ID, session)
        }

        private fun FirSimpleFunction.isDefaultJavaMethod(): Boolean =
            when {
                isIntersectionOverride ->
                    baseForIntersectionOverride!!.isDefaultJavaMethod()
                isSubstitutionOverride ->
                    originalForSubstitutionOverride!!.isDefaultJavaMethod()
                else -> {
                    // Check that we have a non-abstract method from Java interface
                    isJavaOrEnhancement && modality == Modality.OPEN
                }
            }
    }
}
