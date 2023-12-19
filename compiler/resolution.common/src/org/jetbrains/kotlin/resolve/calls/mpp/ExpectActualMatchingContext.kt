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
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCheckingCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker
import org.jetbrains.kotlin.types.model.TypeSystemContext

interface ExpectActualMatchingContext<T : DeclarationSymbolMarker> : TypeSystemContext {
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

    // Default params are not checked on backend because we want to keep "default params in actual" to be suppressible
    // with @Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS") but backend errors are not suppressible (KT-60426)
    // Known clients that do suppress:
    // - stdlib
    // - coroutines
    val shouldCheckDefaultParams: Boolean

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
    val RegularClassSymbolMarker.superTypesRefs: List<TypeRefMarker>
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

    val PropertySymbolMarker.getter: FunctionSymbolMarker?
    val PropertySymbolMarker.setter: FunctionSymbolMarker?

    fun createExpectActualTypeParameterSubstitutor(
        expectActualTypeParameters: List<Pair<TypeParameterSymbolMarker, TypeParameterSymbolMarker>>,
        parentSubstitutor: TypeSubstitutorMarker?
    ): TypeSubstitutorMarker

    fun RegularClassSymbolMarker.collectAllMembers(isActualDeclaration: Boolean): List<DeclarationSymbolMarker>
    fun RegularClassSymbolMarker.getMembersForExpectClass(name: Name): List<DeclarationSymbolMarker>

    fun RegularClassSymbolMarker.collectEnumEntryNames(): List<Name>
    fun RegularClassSymbolMarker.collectEnumEntries(): List<DeclarationSymbolMarker>

    val CallableSymbolMarker.dispatchReceiverType: KotlinTypeMarker?
    val CallableSymbolMarker.extensionReceiverType: KotlinTypeMarker?
    val CallableSymbolMarker.extensionReceiverTypeRef: TypeRefMarker?
    val CallableSymbolMarker.returnType: KotlinTypeMarker
    val CallableSymbolMarker.returnTypeRef: TypeRefMarker
    val CallableSymbolMarker.typeParameters: List<TypeParameterSymbolMarker>
    val FunctionSymbolMarker.valueParameters: List<ValueParameterSymbolMarker>

    /**
     * Returns all symbols that are overridden by [this] symbol, including self
     */
    fun FunctionSymbolMarker.allRecursivelyOverriddenDeclarationsIncludingSelf(containingClass: RegularClassSymbolMarker?): List<CallableSymbolMarker>

    val CallableSymbolMarker.valueParameters: List<ValueParameterSymbolMarker>
        get() = (this as? FunctionSymbolMarker)?.valueParameters ?: emptyList()

    val ValueParameterSymbolMarker.isVararg: Boolean
    val ValueParameterSymbolMarker.isNoinline: Boolean
    val ValueParameterSymbolMarker.isCrossinline: Boolean
    val ValueParameterSymbolMarker.hasDefaultValue: Boolean
    val ValueParameterSymbolMarker.hasDefaultValueNonRecursive: Boolean

    fun CallableSymbolMarker.isAnnotationConstructor(): Boolean

    val TypeParameterSymbolMarker.bounds: List<KotlinTypeMarker>
    val TypeParameterSymbolMarker.boundsTypeRefs: List<TypeRefMarker>
    val TypeParameterSymbolMarker.variance: Variance
    val TypeParameterSymbolMarker.isReified: Boolean

    fun areCompatibleExpectActualTypes(
        expectType: KotlinTypeMarker?,
        actualType: KotlinTypeMarker?,
        parameterOfAnnotationComparisonMode: Boolean = false,
        dynamicTypesEqualToAnything: Boolean = true
    ): Boolean

    fun actualTypeIsSubtypeOfExpectType(
        expectType: KotlinTypeMarker,
        actualType: KotlinTypeMarker
    ): Boolean

    fun RegularClassSymbolMarker.isNotSamInterface(): Boolean

    fun CallableSymbolMarker.isFakeOverride(containingExpectClass: RegularClassSymbolMarker?): Boolean

    val CallableSymbolMarker.isDelegatedMember: Boolean

    val CallableSymbolMarker.hasStableParameterNames: Boolean

    val CallableSymbolMarker.isJavaField: Boolean

    fun onMatchedMembers(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbol: DeclarationSymbolMarker,
        containingExpectClassSymbol: RegularClassSymbolMarker?,
        containingActualClassSymbol: RegularClassSymbolMarker?,
    ) {}

    fun onIncompatibleMembersFromClassScope(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbolsByIncompatibility: Map<ExpectActualCheckingCompatibility.Incompatible<*>, List<DeclarationSymbolMarker>>,
        containingExpectClassSymbol: RegularClassSymbolMarker?,
        containingActualClassSymbol: RegularClassSymbolMarker?,
    ) {}

    fun onMismatchedMembersFromClassScope(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbolsByIncompatibility: Map<ExpectActualMatchingCompatibility.Mismatch, List<DeclarationSymbolMarker>>,
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
     * Whether it is needed to check getters and setters in [AbstractExpectActualAnnotationMatchChecker].
     */
    val checkPropertyAccessorsForAnnotationsCompatibility: Boolean
        get() = true

    /**
     * Whether it is needed to check enum entries in [AbstractExpectActualAnnotationMatchChecker].
     */
    val checkEnumEntriesForAnnotationsCompatibility: Boolean
        get() = true

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
    ): Map<out DeclarationSymbolMarker, ExpectActualMatchingCompatibility>

    fun DeclarationSymbolMarker.getSourceElement(): SourceElementMarker

    fun TypeRefMarker.getClassId(): ClassId?

    /**
     * Callback interface to be implemented by caller of [checkAnnotationsOnTypeRefAndArguments].
     */
    fun interface AnnotationsCheckerCallback {
        /**
         * Implementation must check `expect` and `actual` annotations and report diagnostic in case of incompatibility.
         * [actualTypeRefSource] is needed in order to know where on the `actual` declaration to insert the missing annotation
         * from the `expect` declaration (see [AbstractExpectActualAnnotationMatchChecker.Incompatibility.actualAnnotationTargetElement]).
         */
        fun check(
            expectAnnotations: List<AnnotationCallInfo>, actualAnnotations: List<AnnotationCallInfo>,
            actualTypeRefSource: SourceElementMarker,
        )
    }

    /**
     * Finds pairs of matching expect and actual types, on which annotations must be checked by [AbstractExpectActualAnnotationMatchChecker].
     *
     * This is done by recursively traversing [expectTypeRef] and [actualTypeRef] and their arguments, which is needed in case of
     * complex types like `T1<T2<@Ann T3>>`. Founded expect and actual annotations are passed to [checker] callback.
     * For functional types (e.g. `ReceiverType.(Arg1Type) -> ReturnType`) receiver, argument and return types and their arguments
     * are checked.
     *
     * **Example**: for type `@Ann1 List<@Ann2 Map<@Ann3 Int, @Ann4 String>>`, there are 4 types to check in [checker].
     */
    fun checkAnnotationsOnTypeRefAndArguments(
        expectContainingSymbol: DeclarationSymbolMarker,
        actualContainingSymbol: DeclarationSymbolMarker,
        expectTypeRef: TypeRefMarker,
        actualTypeRef: TypeRefMarker,
        checker: AnnotationsCheckerCallback,
    )
}
