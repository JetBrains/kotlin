/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.mpp

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.mpp.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker
import org.jetbrains.kotlin.types.model.TypeSystemContext

interface ExpectActualMatchingContext<T : DeclarationSymbolMarker> : TypeSystemContext {
    val shouldCheckReturnTypesOfCallables: Boolean

    /*
     * This flag indicates how are type parameters of inner classes stored in the specific implementation of RegularClassSymbolMarker
     *
     * class Outer<T> {
     *     inner class Inner<U>
     * }
     *
     * If flag is set to `true` then `typeParameters` for class `Outer.Inner` contains both parameters: [U, R]
     * Otherwise it contains only parameters of itself: [U]
     *
     * This flag is needed for proper calculation of substitutions for components of inner classes
     */
    val innerClassesCapturesOuterTypeParameters: Boolean
        get() = true

    val enumConstructorsAreAlwaysCompatible: Boolean
        get() = false

    // Try to drop it once KT-61105 is fixed
    val shouldCheckAbsenceOfDefaultParamsInActual: Boolean

    /**
     * This flag determines, how visibilities for classes/typealiases will be matched
     * - `false` means that visibilities should be identical
     * - `true` means that visibility of actual class should be the same or wider comparing to expect visibility
     *     this means that following actualizations will be additionally allowed:
     *     - protected -> public
     *     - internal -> public
     */
    val allowClassActualizationWithWiderVisibility: Boolean
        get() = false

    /**
     * This flag determines strategy for matching supertypes between expect and actual class
     *  - `false` means that expect and actual supertypes are matched one by one
     *  - `true` means that type of actual class should be subtype of each expect supertype of the expect class
     */
    val allowTransitiveSupertypesActualization: Boolean
        get() = false

    val RegularClassSymbolMarker.classId: ClassId
    val TypeAliasSymbolMarker.classId: ClassId
    val CallableSymbolMarker.callableId: CallableId
    val TypeParameterSymbolMarker.parameterName: Name
    val ValueParameterSymbolMarker.parameterName: Name

    fun TypeAliasSymbolMarker.expandToRegularClass(): RegularClassSymbolMarker?

    val RegularClassSymbolMarker.classKind: ClassKind

    val RegularClassSymbolMarker.isCompanion: Boolean
    val RegularClassSymbolMarker.isInner: Boolean
    val RegularClassSymbolMarker.isInline: Boolean
    val RegularClassSymbolMarker.isValue: Boolean
    val RegularClassSymbolMarker.isFun: Boolean
    val ClassLikeSymbolMarker.typeParameters: List<TypeParameterSymbolMarker>

    val ClassLikeSymbolMarker.modality: Modality?
    val ClassLikeSymbolMarker.visibility: Visibility

    val CallableSymbolMarker.modality: Modality?
    val CallableSymbolMarker.visibility: Visibility

    val RegularClassSymbolMarker.superTypes: List<KotlinTypeMarker>
    val RegularClassSymbolMarker.defaultType: KotlinTypeMarker

    val CallableSymbolMarker.isExpect: Boolean
    val CallableSymbolMarker.isInline: Boolean
    val CallableSymbolMarker.isSuspend: Boolean
    val CallableSymbolMarker.isExternal: Boolean
    val CallableSymbolMarker.isInfix: Boolean
    val CallableSymbolMarker.isOperator: Boolean
    val CallableSymbolMarker.isTailrec: Boolean

    val PropertySymbolMarker.isVar: Boolean
    val PropertySymbolMarker.isLateinit: Boolean
    val PropertySymbolMarker.isConst: Boolean

    val PropertySymbolMarker.setter: FunctionSymbolMarker?

    fun createExpectActualTypeParameterSubstitutor(
        expectTypeParameters: List<TypeParameterSymbolMarker>,
        actualTypeParameters: List<TypeParameterSymbolMarker>,
        parentSubstitutor: TypeSubstitutorMarker?
    ): TypeSubstitutorMarker

    fun RegularClassSymbolMarker.collectAllMembers(isActualDeclaration: Boolean): List<DeclarationSymbolMarker>
    fun RegularClassSymbolMarker.getMembersForExpectClass(name: Name): List<DeclarationSymbolMarker>

    fun RegularClassSymbolMarker.collectEnumEntryNames(): List<Name>
    fun RegularClassSymbolMarker.collectEnumEntries(): List<DeclarationSymbolMarker>

    val CallableSymbolMarker.dispatchReceiverType: KotlinTypeMarker?
    val CallableSymbolMarker.extensionReceiverType: KotlinTypeMarker?
    val CallableSymbolMarker.returnType: KotlinTypeMarker
    val CallableSymbolMarker.typeParameters: List<TypeParameterSymbolMarker>
    val FunctionSymbolMarker.valueParameters: List<ValueParameterSymbolMarker>

    /**
     * Returns all symbols that are overridden by [this] symbol
     */
    fun FunctionSymbolMarker.allOverriddenDeclarationsRecursive(): Sequence<CallableSymbolMarker>

    val CallableSymbolMarker.valueParameters: List<ValueParameterSymbolMarker>
        get() = (this as? FunctionSymbolMarker)?.valueParameters ?: emptyList()

    val ValueParameterSymbolMarker.isVararg: Boolean
    val ValueParameterSymbolMarker.isNoinline: Boolean
    val ValueParameterSymbolMarker.isCrossinline: Boolean
    val ValueParameterSymbolMarker.hasDefaultValue: Boolean

    fun CallableSymbolMarker.isAnnotationConstructor(): Boolean

    val TypeParameterSymbolMarker.bounds: List<KotlinTypeMarker>
    val TypeParameterSymbolMarker.variance: Variance
    val TypeParameterSymbolMarker.isReified: Boolean

    fun areCompatibleExpectActualTypes(
        expectType: KotlinTypeMarker?,
        actualType: KotlinTypeMarker?,
    ): Boolean

    fun actualTypeIsSubtypeOfExpectType(
        expectType: KotlinTypeMarker,
        actualType: KotlinTypeMarker
    ): Boolean

    fun RegularClassSymbolMarker.isNotSamInterface(): Boolean

    /*
     * Determines should some declaration from expect class scope be checked
     *  - FE 1.0: skip fake overrides
     *  - FIR: skip fake overrides
     *  - IR: skip nothing
     */
    fun CallableSymbolMarker.shouldSkipMatching(containingExpectClass: RegularClassSymbolMarker): Boolean

    val CallableSymbolMarker.hasStableParameterNames: Boolean

    fun onMatchedMembers(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbol: DeclarationSymbolMarker,
        containingExpectClassSymbol: RegularClassSymbolMarker?,
        containingActualClassSymbol: RegularClassSymbolMarker?,
    ) {}

    fun onMismatchedMembersFromClassScope(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbolsByIncompatibility: Map<ExpectActualCompatibility.Incompatible<*>, List<DeclarationSymbolMarker>>,
        containingExpectClassSymbol: RegularClassSymbolMarker?,
        containingActualClassSymbol: RegularClassSymbolMarker?,
    ) {}

    val DeclarationSymbolMarker.annotations: List<AnnotationCallInfo>

    fun areAnnotationArgumentsEqual(
        expectAnnotation: AnnotationCallInfo,
        actualAnnotation: AnnotationCallInfo,
        collectionArgumentsCompatibilityCheckStrategy: ExpectActualCollectionArgumentsCompatibilityCheckStrategy,
    ): Boolean

    val DeclarationSymbolMarker.hasSourceAnnotationsErased: Boolean

    interface AnnotationCallInfo {
        val annotationSymbol: Any
        val classId: ClassId?
        val isRetentionSource: Boolean
        val isOptIn: Boolean
    }

    val checkClassScopesForAnnotationCompatibility: Boolean

    /**
     * Determines whether it is needed to skip checking annotations on class member in [AbstractExpectActualAnnotationMatchChecker].
     *
     * This is needed to prevent checking member twice if it is real `actual` member (not fake override or member of
     * class being typealiased).
     * Example:
     * ```
     * actual class A {
     *   actual fun foo() {} // 1: checked itself, 2: checked as member of A
     * }
     * ```
     */
    fun skipCheckingAnnotationsOfActualClassMember(actualMember: DeclarationSymbolMarker): Boolean

    fun findPotentialExpectClassMembersForActual(
        expectClass: RegularClassSymbolMarker,
        actualClass: RegularClassSymbolMarker,
        actualMember: DeclarationSymbolMarker,
        checkClassScopesCompatibility: Boolean,
    ): Map<out DeclarationSymbolMarker, ExpectActualCompatibility<*>>
}
