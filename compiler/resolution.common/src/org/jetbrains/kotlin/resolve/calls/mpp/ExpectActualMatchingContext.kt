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

    val CallableSymbolMarker.dispatchReceiverType: KotlinTypeMarker?
    val CallableSymbolMarker.extensionReceiverType: KotlinTypeMarker?
    val CallableSymbolMarker.returnType: KotlinTypeMarker
    val CallableSymbolMarker.typeParameters: List<TypeParameterSymbolMarker>
    val FunctionSymbolMarker.valueParameters: List<ValueParameterSymbolMarker>

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

    fun RegularClassSymbolMarker.isNotSamInterface(): Boolean

    /*
     * Determines should some declaration from expect class scope be checked
     *  - FE 1.0: skip fake overrides
     *  - FIR: skip fake overrides
     *  - IR: skip nothing
     */
    fun CallableSymbolMarker.shouldSkipMatching(containingExpectClass: RegularClassSymbolMarker): Boolean

    val CallableSymbolMarker.hasStableParameterNames: Boolean

    fun onMatchedMembers(expectSymbol: DeclarationSymbolMarker, actualSymbol: DeclarationSymbolMarker) {}

    fun onMismatchedMembersFromClassScope(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbolsByIncompatibility: Map<ExpectActualCompatibility.Incompatible<*>, List<DeclarationSymbolMarker>>
    ) {}
}
