/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.config.LanguageFeature.ForbidSyntheticPropertiesWithoutBaseJavaGetter
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.buildSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticPropertiesScope.SyntheticGetterCompatibility.*
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.SyntheticSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.AbstractTypeChecker

/**
 * This symbol is bound to a synthetic property based on Java getter/setter call
 *
 * See details about such properties here: https://kotlinlang.org/docs/java-interop.html#getters-and-setters).
 * Frontend IR creates this kind of symbol each time when x.foo should be resolved to x.getFoo() or x.setFoo().
 */
class FirSimpleSyntheticPropertySymbol(
    propertyId: CallableId,
    getterId: CallableId
) : FirSyntheticPropertySymbol(propertyId, getterId), SyntheticSymbol {
    override fun copy(): FirSyntheticPropertySymbol = FirSimpleSyntheticPropertySymbol(callableId, getterId)
}

class FirSyntheticFunctionSymbol(
    callableId: CallableId
) : FirNamedFunctionSymbol(callableId), SyntheticSymbol

class FirSyntheticPropertiesScope private constructor(
    val session: FirSession,
    private val baseScope: FirTypeScope,
    private val dispatchReceiverType: ConeKotlinType,
    private val syntheticNamesProvider: FirSyntheticNamesProvider,
    private val returnTypeCalculator: ReturnTypeCalculator?,
    private val isSuperCall: Boolean,
) : FirContainingNamesAwareScope() {
    companion object {
        fun createIfSyntheticNamesProviderIsDefined(
            session: FirSession,
            dispatchReceiverType: ConeKotlinType,
            baseScope: FirTypeScope,
            returnTypeCalculator: ReturnTypeCalculator? = null,
            isSuperCall: Boolean = false,
        ): FirSyntheticPropertiesScope? {
            val syntheticNamesProvider = session.syntheticNamesProvider ?: return null
            return FirSyntheticPropertiesScope(
                session,
                baseScope,
                dispatchReceiverType,
                syntheticNamesProvider,
                returnTypeCalculator,
                isSuperCall,
            )
        }
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        val getterNames = syntheticNamesProvider.possibleGetterNamesByPropertyName(name)
        var getterFound = false
        for (getterName in getterNames) {
            baseScope.processFunctionsByName(getterName) {
                checkGetAndCreateSynthetic(name, getterName, it, needCheckForSetter = true, processor)
                getterFound = true
            }
        }
        if (!getterFound && shouldSearchForJavaRecordComponents()) {
            baseScope.processFunctionsByName(name) {
                if (it.fir.isJavaRecordComponent == true) {
                    checkGetAndCreateSynthetic(name, name, it, needCheckForSetter = false, processor)
                }
            }
        }
    }

    private fun shouldSearchForJavaRecordComponents(): Boolean {
        /*
         * Fast path: if dispatch receiver type is simple type and corresponding
         *   class is not a java record then there is no need to additional
         *   search for record components
         */
        val dispatchSymbol = dispatchReceiverType.toRegularClassSymbol(session) ?: return true
        return dispatchSymbol.fir.isJavaRecord ?: false
    }

    override fun getCallableNames(): Set<Name> = baseScope.getCallableNames().flatMapTo(hashSetOf()) { propertyName ->
        syntheticNamesProvider.possiblePropertyNamesByAccessorName(propertyName)
    }

    override fun getClassifierNames(): Set<Name> = emptySet()

    private fun checkGetAndCreateSynthetic(
        propertyName: Name,
        getterName: Name,
        getterSymbol: FirNamedFunctionSymbol,
        needCheckForSetter: Boolean,
        processor: (FirVariableSymbol<*>) -> Unit
    ) {
        val getter = getterSymbol.fir

        if (getter.typeParameters.isNotEmpty()) return
        if (getter.valueParameters.isNotEmpty()) return
        if (getter.isStatic) return

        var getterReturnType = (getter.returnTypeRef as? FirResolvedTypeRef)?.type
        if (getterReturnType == null && needCheckForSetter) {
            // During implicit body resolve phase, we can encounter a reference to a not yet resolved Kotlin class that inherits a
            // synthetic property from a Java class. In that case, resolve the return type here, ignoring error types (e.g. cycles).
            getterReturnType = returnTypeCalculator?.tryCalculateReturnTypeOrNull(getter)?.type?.takeUnless { it is ConeErrorType }
        }

        // `void` type is the only case when we've got not-nullable non-enhanced Unit from Java
        // And it doesn't make sense to make a synthetic property for `void` typed getters
        if (getterReturnType?.isUnit == true && CompilerConeAttributes.EnhancedNullability !in getterReturnType.attributes) return

        // Should have Java among overridden _and_ don't have isHiddenEverywhereBesideSuperCalls among them
        val (getterCompatibility, deprecatedOverrideOfHidden) = getterSymbol.computeGetterCompatibility()
        if (getterCompatibility == Incompatible) return

        var matchingSetter: FirSimpleFunction? = null
        if (needCheckForSetter && getterReturnType != null) {
            val setterName = syntheticNamesProvider.setterNameByGetterName(getterName)
            baseScope.processFunctionsByName(setterName, fun(setterSymbol: FirNamedFunctionSymbol) {
                if (matchingSetter != null) return

                val setter = setterSymbol.fir
                if (setter.typeParameters.isNotEmpty() || setter.isStatic) return
                val parameter = setter.valueParameters.singleOrNull() ?: return
                if (parameter.isVararg) return
                val parameterType = (parameter.returnTypeRef as? FirResolvedTypeRef)?.type ?: return
                if (!setterTypeIsConsistentWithGetterType(propertyName, getterSymbol, setterSymbol, parameterType)) return
                matchingSetter = setterSymbol.fir
            })
        }

        val property = buildSyntheticProperty(propertyName, getter, matchingSetter, getterCompatibility, deprecatedOverrideOfHidden)
        getter.originalForSubstitutionOverride?.let {
            property.originalForSubstitutionOverrideAttr = buildSyntheticProperty(
                propertyName,
                it,
                matchingSetter?.originalForSubstitutionOverride ?: matchingSetter,
                getterCompatibility,
                deprecatedOverrideOfHidden,
            )
        }
        val syntheticSymbol = property.symbol
        (baseScope as? FirUnstableSmartcastTypeScope)?.apply {
            if (isSymbolFromUnstableSmartcast(getterSymbol)) {
                markSymbolFromUnstableSmartcast(syntheticSymbol)
            }
        }
        processor(syntheticSymbol)
    }

    private fun buildSyntheticProperty(
        propertyName: Name,
        getter: FirSimpleFunction,
        setter: FirSimpleFunction?,
        getterCompatibility: SyntheticGetterCompatibility,
        deprecatedOverrideOfHidden: Boolean,
    ): FirSyntheticProperty {
        val classLookupTag = getter.symbol.originalOrSelf().dispatchReceiverClassLookupTagOrNull()
        val packageName = classLookupTag?.classId?.packageFqName ?: getter.symbol.callableId.packageName
        val className = classLookupTag?.classId?.relativeClassName

        return buildSyntheticProperty {
            moduleData = session.moduleData
            name = propertyName
            symbol = FirSimpleSyntheticPropertySymbol(
                getterId = getter.symbol.callableId,
                propertyId = CallableId(packageName, className, propertyName)
            )
            delegateGetter = getter
            delegateSetter = setter
            deprecationsProvider = getDeprecationsProviderFromAccessors(session, getter, setter)
        }.apply {
            if (getterCompatibility != HasJavaOrigin) {
                noJavaOrigin = true
            }
            if (deprecatedOverrideOfHidden) {
                this.deprecatedOverrideOfHidden = true
            }
        }
    }

    private fun setterTypeIsConsistentWithGetterType(
        propertyName: Name,
        getterSymbol: FirNamedFunctionSymbol,
        setterSymbol: FirNamedFunctionSymbol,
        setterParameterType: ConeKotlinType
    ): Boolean {
        val getterReturnType = getterSymbol.resolvedReturnTypeRef.type
        if (AbstractTypeChecker.equalTypes(session.typeContext, getterReturnType, setterParameterType)) return true
        if (!AbstractTypeChecker.isSubtypeOf(session.typeContext, getterReturnType, setterParameterType)) return false

        /*
         * If type of setter parameter is subtype of getter return type, we need to check corresponding "overridden" synthetic
         *   properties from parent classes. If some of them has this setter or its overridden as base for setter, then current
         *   setterSymbol can be used as setter for corresponding getterSymbol
         *
         * See corresponding code in FE 1.0 in `SyntheticJavaPropertyDescriptor.isGoodSetMethod`
         * Note that FE 1.0 looks through overrides just ones (by setter hierarchy), but FIR does twice (for setter and getter)
         *   This is needed because FIR does not create fake overrides for all inherited methods of class, so there may be a
         *   situation, when in inheritor class only getter is overridden, and setter does not have overriddens at all
         *
         * class Base {
         *     public Object getX() {...}
         *     public Object setX(Object x) {...} // setterSymbol
         * }
         *
         * class Derived extends Base {
         *     public String getX() {...} // getterSymbol
         * //  public fake-override Object setX(Object x) {...} // exist in FE 1.0 but not in FIR
         * }
         */

        fun processOverrides(symbolToStart: FirNamedFunctionSymbol, setterSymbolToCompare: FirNamedFunctionSymbol?): Boolean {
            var hasMatchingSetter = false
            baseScope.processDirectOverriddenFunctionsWithBaseScope(symbolToStart) l@{ symbol, scope ->
                if (hasMatchingSetter) return@l ProcessorAction.STOP
                val baseDispatchReceiverType = symbol.dispatchReceiverType ?: return@l ProcessorAction.NEXT
                val syntheticScope = FirSyntheticPropertiesScope(session, scope, baseDispatchReceiverType, syntheticNamesProvider, returnTypeCalculator, isSuperCall)
                val baseProperties = syntheticScope.getProperties(propertyName)
                val propertyFound = baseProperties.any {
                    val baseProperty = it.fir
                    baseProperty is FirSyntheticProperty && baseProperty.setter?.delegate?.symbol == (setterSymbolToCompare ?: symbol)
                }
                if (propertyFound) {
                    hasMatchingSetter = true
                    ProcessorAction.STOP
                } else {
                    ProcessorAction.NEXT
                }
            }
            return hasMatchingSetter
        }

        return processOverrides(setterSymbol, setterSymbolToCompare = null)
                || processOverrides(getterSymbol, setterSymbolToCompare = setterSymbol)
    }

    private enum class SyntheticGetterCompatibility {
        Incompatible,
        HasKotlinOrigin,
        HasJavaOrigin
    }

    private data class GetterCompatibilityResult(val compatibility: SyntheticGetterCompatibility, val deprecatedOverrideOfHidden: Boolean)
    /**
     * This method computes if getter method can be used as base for synthetic property based on overridden hierarchy
     * There are three kinds of compatibility:
     * - `Incompatible` (obvious)
     * - `HasJavaOrigin` indicates that this getter is based on root java function (ok to create property)
     * - `HasKotlinOrigin` shows that there is no base java getter overridden. Property will be created only with some LV (KT-64358)
     */
    private fun FirNamedFunctionSymbol.computeGetterCompatibility(): GetterCompatibilityResult {
        val kotlinBaseAllowed = !session.languageVersionSettings.supportsFeature(ForbidSyntheticPropertiesWithoutBaseJavaGetter)

        var isHiddenEverywhereBesideSuperCalls = false
        var isDeprecatedOverrideOfHidden = false
        var result = Incompatible

        val visited = mutableSetOf<MemberWithBaseScope<FirNamedFunctionSymbol>>()
        fun checkJavaOrigin(symbol: FirNamedFunctionSymbol, scope: FirTypeScope, isOverridden: Boolean) {
            val hidden = symbol.hiddenStatusOfCall(isSuperCall = isSuperCall, isCallToOverride = isOverridden)
            when (hidden) {
                CallToPotentiallyHiddenSymbolResult.Hidden -> isHiddenEverywhereBesideSuperCalls = true
                CallToPotentiallyHiddenSymbolResult.VisibleWithDeprecation -> isDeprecatedOverrideOfHidden = true
                CallToPotentiallyHiddenSymbolResult.Visible -> {}
            }

            val overriddenWithScope = scope.getDirectOverriddenFunctionsWithBaseScope(symbol)

            if (symbol.origin == FirDeclarationOrigin.Enhancement) {
                /**
                 * If there is no overridden then we found java root and want to stick with it
                 * Otherwise we are in the middle of the hierarchy, so leaf potentially can be from Kotlin
                 */
                val potentialResult = when {
                    overriddenWithScope.isEmpty() -> HasJavaOrigin
                    kotlinBaseAllowed -> HasKotlinOrigin
                    else -> Incompatible
                }
                result = maxOf(result, potentialResult)
            }

            overriddenWithScope.forEach {
                if (!visited.add(it)) return@forEach
                checkJavaOrigin(it.member, it.baseScope, isOverridden = true)
            }
        }

        checkJavaOrigin(this, baseScope, isOverridden = false)

        val syntheticGetterCompatibility = when {
            isHiddenEverywhereBesideSuperCalls -> Incompatible
            result != Incompatible -> result
            !kotlinBaseAllowed -> Incompatible
            // This branch is needed for compatibility reasons, see KT-64358
            isJavaTypeOnThePath(this.dispatchReceiverType) -> HasKotlinOrigin
            else -> Incompatible
        }
        return GetterCompatibilityResult(syntheticGetterCompatibility, isDeprecatedOverrideOfHidden)
    }

    /*
     * We should check the whole hierarchy between dispatch receiver type of found getter and our dispatch receiver type, because
     *   synthetic properly should be created if there is a java class on this way (KT-62394)
     *
     * // FILE: Base.kt
     * abstract class Base(private val x: String) {
     *     fun getFoo() = x
     * }
     *
     * // FILE: Intermediate.java
     * public class Intermediate extends Base {
     *     public Intermediate(String x) {
     *         super(x);
     *     }
     * }
     *
     * // FILE: Final.kt
     * class Final(x: String) : Intermediate(x)
     *
     * fun test(f: Final) {
     *     f.foo // <-------
     * }
     */
    private fun isJavaTypeOnThePath(baseType: ConeSimpleKotlinType?): Boolean {
        val lookupTagToStop = (baseType as? ConeLookupTagBasedType)?.lookupTag ?: return false
        val dispatchReceiverClassSymbol = (dispatchReceiverType as? ConeLookupTagBasedType)?.lookupTag?.toSymbol(session) ?: return false

        val typeContext = session.typeContext
        fun checkType(type: ConeClassLikeType): Boolean {
            val state = typeContext.newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = false)
            if (type.toRegularClassSymbol(session)?.isJavaOrEnhancement == true) {
                if (AbstractTypeChecker.isSubtypeOfClass(state, type.lookupTag, lookupTagToStop)) {
                    return true
                }
            }
            return false
        }

        if (dispatchReceiverType is ConeClassLikeType && checkType(dispatchReceiverType)) {
            return true
        }

        when (dispatchReceiverClassSymbol) {
            is FirClassLikeSymbol -> {
                val superTypes = lookupSuperTypes(dispatchReceiverClassSymbol, lookupInterfaces = true, deep = true, session)
                for (superType in superTypes) {
                    if (checkType(superType)) return true
                }
                return false
            }
            is FirTypeParameterSymbol -> {
                return false
            }
        }
    }
}

private object NoJavaOriginKey : FirDeclarationDataKey()
private var FirSyntheticProperty.noJavaOrigin: Boolean? by FirDeclarationDataRegistry.data(NoJavaOriginKey)

val FirSimpleSyntheticPropertySymbol.noJavaOrigin: Boolean
    get() = (fir as FirSyntheticProperty).noJavaOrigin == true

private object DeprecatedOverrideOfHidden : FirDeclarationDataKey()
private var FirSyntheticProperty.deprecatedOverrideOfHidden: Boolean? by FirDeclarationDataRegistry.data(DeprecatedOverrideOfHidden)

val FirSimpleSyntheticPropertySymbol.deprecatedOverrideOfHidden: Boolean
    get() = (fir as FirSyntheticProperty).deprecatedOverrideOfHidden == true

