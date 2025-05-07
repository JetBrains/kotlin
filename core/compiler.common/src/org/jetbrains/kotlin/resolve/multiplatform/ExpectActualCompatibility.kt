/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform

import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility.Mismatch

private const val TYPE_PARAMETER_COUNT = "the number of type parameters are different"
private const val TYPE_PARAMETER_UPPER_BOUNDS = "the upper bounds of type parameters are different"

// Note that the reason is used in the diagnostic output, see PlatformIncompatibilityDiagnosticRenderer
/**
 * DON'T USE THIS CLASS. This class is currently used only in diagnostics. Eventually, it will go away KT-62631
 */
sealed interface ExpectActualCompatibility<out D> {
    /**
     * DON'T USE THIS CLASS. This class is currently used only in diagnostics. Eventually, it will go away KT-62631
     */
    sealed interface MismatchOrIncompatible<out D> : ExpectActualCompatibility<D> {
        val reason: String
    }
}

/**
 * All mismatches that can be fixed by introducing an overload without this mismatch.
 * In other words: "overloadable" mismatches
 *
 * @see ExpectActualCheckingCompatibility
 */
sealed class ExpectActualMatchingCompatibility : ExpectActualCompatibility<Nothing> {
    sealed class Mismatch(override val reason: String) : ExpectActualMatchingCompatibility(),
        ExpectActualCompatibility.MismatchOrIncompatible<Nothing>

    object CallableKind : Mismatch("callable kinds are different (function vs property)")
    object ActualJavaField : Mismatch("actualization to Java field is prohibited")
    object ParameterShape : Mismatch("parameter shapes are different (extension vs non-extension)")
    object ParameterCount : Mismatch("number of value parameters are different")
    object ContextParameterCount : Mismatch("number of context parameters are different")
    object FunctionTypeParameterCount : Mismatch(TYPE_PARAMETER_COUNT)
    object ParameterTypes : Mismatch("parameter types are different")
    object ContextParameterTypes : Mismatch("context parameter types are different")
    object FunctionTypeParameterUpperBounds : Mismatch(TYPE_PARAMETER_UPPER_BOUNDS)
    object MatchedSuccessfully : ExpectActualMatchingCompatibility()
}

/**
 * "Non-overloadable" compatibilities
 *
 * @see ExpectActualMatchingCompatibility
 */
sealed class ExpectActualCheckingCompatibility<out D> : ExpectActualCompatibility<D> {
    sealed class Incompatible<out D>(override val reason: String) : ExpectActualCheckingCompatibility<D>(),
        ExpectActualCompatibility.MismatchOrIncompatible<D>

    object ClassTypeParameterCount : Incompatible<Nothing>(TYPE_PARAMETER_COUNT)

    // Callables
    object ReturnType : Incompatible<Nothing>("the return types are different")
    object ParameterNames : Incompatible<Nothing>("the parameter names are different")
    object ContextParameterNames : Incompatible<Nothing>("the context parameter names are different")
    object TypeParameterNames : Incompatible<Nothing>("the names of type parameters are different")
    object ValueParameterVararg : Incompatible<Nothing>("some value parameter is vararg in one declaration and non-vararg in the other")
    object ValueParameterNoinline :
        Incompatible<Nothing>("some value parameter is noinline in one declaration and not noinline in the other")
    object ValueParameterCrossinline :
        Incompatible<Nothing>("some value parameter is crossinline in one declaration and not crossinline in the other")

    // Functions
    object FunctionModifiersDifferent : Incompatible<Nothing>("the modifiers are different (suspend)")
    object FunctionModifiersNotSubset :
        Incompatible<Nothing>("some modifiers on 'expect' declaration are missing on the 'actual' one (infix, inline, operator)")
    object ActualFunctionWithDefaultParameters :
        Incompatible<Nothing>("the 'actual' function cannot have default argument values, they should be declared in the 'expect' function")
    object DefaultArgumentsInExpectActualizedByFakeOverride :
        Incompatible<Nothing>("default argument values inside 'expect' declaration are not allowed for methods actualized via fake override")

    // Properties
    object PropertyKind : Incompatible<Nothing>("the property kinds are different (val vs var)")
    object PropertyLateinitModifier : Incompatible<Nothing>("the modifiers are different (lateinit)")
    object PropertyConstModifier : Incompatible<Nothing>("the modifiers are different (const)")
    class PropertySetterVisibility(
        expectVisibility: org.jetbrains.kotlin.descriptors.Visibility?,
        actualVisibility: org.jetbrains.kotlin.descriptors.Visibility?,
    ) : Incompatible<Nothing>(
        """
            the setter visibilities are different.
              The 'expect' declaration setter visibility: '${expectVisibility?.name}'
              The 'actual' declaration setter visibility: '${actualVisibility?.name}'
        """.trimIndent()
    )

    // Classifiers
    object ClassKind : Incompatible<Nothing>("the class kinds are different (class, interface, object, enum, annotation)")
    object ClassModifiers : Incompatible<Nothing>("the modifiers are different (companion, inner, inline, value)")
    object FunInterfaceModifier : Incompatible<Nothing>("the 'actual' declaration for 'fun expect interface' is not a functional interface")
    object Supertypes : Incompatible<Nothing>("some supertypes are missing in the 'actual' declaration")
    object NestedTypeAlias : Incompatible<Nothing>("actualization by nested type alias is prohibited")
    class ClassScopes<D>(
        val mismatchedMembers: List<Pair</* expect */ D, Map<Mismatch, /* actuals */ Collection<D>>>>,
        val incompatibleMembers: List<MemberIncompatibility<D>>,
    ) : Incompatible<D>("some 'expect' members have no 'actual' ones")
    object EnumEntries : Incompatible<Nothing>("some entries from 'expect enum' are missing in the 'actual enum'")
    object IllegalRequiresOpt : Incompatible<Nothing>("opt-in annotations are prohibited to be 'expect' or 'actual'. Instead, declare annotation once in common sources")

    // Common
    class Modality(
        expectModality: org.jetbrains.kotlin.descriptors.Modality?,
        actualModality: org.jetbrains.kotlin.descriptors.Modality?,
    ) : Incompatible<Nothing>(
        """
            the modalities are different.
              The 'expect' declaration modality: '${expectModality.toString().lowercase()}'
              The 'actual' declaration modality: '${actualModality.toString().lowercase()}'
        """.trimIndent()
    )
    class Visibility(
        expectVisibility: org.jetbrains.kotlin.descriptors.Visibility,
        actualVisibility: org.jetbrains.kotlin.descriptors.Visibility,
    ) : Incompatible<Nothing>(
        """
            the visibilities are different.
              The 'expect' declaration visibility: '${expectVisibility.name}'
              The 'actual' declaration visibility: '${actualVisibility.name}'
        """.trimIndent()
    )

    object ClassTypeParameterUpperBounds : Incompatible<Nothing>(TYPE_PARAMETER_UPPER_BOUNDS)
    object TypeParameterVariance : Incompatible<Nothing>("declaration-site variances of type parameters are different")
    object TypeParameterReified : Incompatible<Nothing>("some type parameter is reified in one declaration and non-reified in the other")
    object Compatible : ExpectActualCheckingCompatibility<Nothing>()
}

data class MemberIncompatibility<out D>(
    val expect: D,
    val actual: D,
    val incompatibility: ExpectActualCheckingCompatibility.Incompatible<D>,
)
