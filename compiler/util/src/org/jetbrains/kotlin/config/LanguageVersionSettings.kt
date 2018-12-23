/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.config.LanguageFeature.Kind.*
import org.jetbrains.kotlin.config.LanguageVersion.*
import org.jetbrains.kotlin.utils.DescriptionAware
import java.util.*

enum class LanguageFeature(
    val sinceVersion: LanguageVersion?,
    val sinceApiVersion: ApiVersion = ApiVersion.KOTLIN_1_0,
    val hintUrl: String? = null,
    val defaultState: State = State.ENABLED,
    val kind: Kind = OTHER // NB: default value OTHER doesn't force pre-releaseness (see KDoc)
) {
    // Note: names of these entries are also used in diagnostic tests and in user-visible messages (see presentableText below)
    TypeAliases(KOTLIN_1_1),
    BoundCallableReferences(KOTLIN_1_1, ApiVersion.KOTLIN_1_1),
    LocalDelegatedProperties(KOTLIN_1_1, ApiVersion.KOTLIN_1_1),
    TopLevelSealedInheritance(KOTLIN_1_1),
    AdditionalBuiltInsMembers(KOTLIN_1_1),
    DataClassInheritance(KOTLIN_1_1),
    InlineProperties(KOTLIN_1_1),
    DestructuringLambdaParameters(KOTLIN_1_1),
    SingleUnderscoreForParameterName(KOTLIN_1_1),
    DslMarkersSupport(KOTLIN_1_1),
    UnderscoresInNumericLiterals(KOTLIN_1_1),
    DivisionByZeroInConstantExpressions(KOTLIN_1_1),
    InlineConstVals(KOTLIN_1_1),
    OperatorRem(KOTLIN_1_1),
    OperatorProvideDelegate(KOTLIN_1_1),
    ShortSyntaxForPropertyGetters(KOTLIN_1_1),
    RefinedSamAdaptersPriority(KOTLIN_1_1),
    SafeCallBoundSmartCasts(KOTLIN_1_1),
    TypeInferenceOnGenericsForCallableReferences(KOTLIN_1_1),
    NoDelegationToJavaDefaultInterfaceMembers(KOTLIN_1_1),
    DefaultImportOfPackageKotlinComparisons(KOTLIN_1_1),

    ArrayLiteralsInAnnotations(KOTLIN_1_2),
    InlineDefaultFunctionalParameters(KOTLIN_1_2),
    SoundSmartCastsAfterTry(KOTLIN_1_2),
    DeprecatedFieldForInvisibleCompanionObject(KOTLIN_1_2),
    NullabilityAssertionOnExtensionReceiver(KOTLIN_1_2),
    SafeCastCheckBoundSmartCasts(KOTLIN_1_2),
    CapturedInClosureSmartCasts(KOTLIN_1_2),
    LateinitTopLevelProperties(KOTLIN_1_2),
    LateinitLocalVariables(KOTLIN_1_2),
    InnerClassInEnumEntryClass(KOTLIN_1_2),
    CallableReferencesToClassMembersWithEmptyLHS(KOTLIN_1_2),
    ThrowNpeOnExplicitEqualsForBoxedNull(KOTLIN_1_2),
    JvmPackageName(KOTLIN_1_2),
    AssigningArraysToVarargsInNamedFormInAnnotations(KOTLIN_1_2),
    ExpectedTypeFromCast(KOTLIN_1_2),
    DefaultMethodsCallFromJava6TargetError(KOTLIN_1_2),

    RestrictionOfValReassignmentViaBackingField(KOTLIN_1_3, kind = BUG_FIX),
    NestedClassesInEnumEntryShouldBeInner(KOTLIN_1_3, kind = BUG_FIX),
    ProhibitDataClassesOverridingCopy(KOTLIN_1_3, kind = BUG_FIX),
    RestrictionOfWrongAnnotationsWithUseSiteTargetsOnTypes(KOTLIN_1_3, kind = BUG_FIX),
    ProhibitInnerClassesOfGenericClassExtendingThrowable(KOTLIN_1_3, kind = BUG_FIX),
    ProperForInArrayLoopRangeVariableAssignmentSemantic(KOTLIN_1_3, kind = BUG_FIX),
    NestedClassesInAnnotations(KOTLIN_1_3),
    JvmStaticInInterface(KOTLIN_1_3, kind = UNSTABLE_FEATURE),
    JvmFieldInInterface(KOTLIN_1_3, kind = UNSTABLE_FEATURE),
    ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion(KOTLIN_1_3, kind = BUG_FIX),
    ProhibitNonConstValuesAsVarargsInAnnotations(KOTLIN_1_3, kind = BUG_FIX),
    ReleaseCoroutines(KOTLIN_1_3, kind = UNSTABLE_FEATURE),
    ReadDeserializedContracts(KOTLIN_1_3),
    UseReturnsEffect(KOTLIN_1_3),
    UseCallsInPlaceEffect(KOTLIN_1_3),
    AllowContractsForCustomFunctions(KOTLIN_1_3),
    VariableDeclarationInWhenSubject(KOTLIN_1_3),
    ProhibitLocalAnnotations(KOTLIN_1_3, kind = BUG_FIX),
    ProhibitSmartcastsOnLocalDelegatedProperty(KOTLIN_1_3, kind = BUG_FIX),
    ProhibitOperatorMod(KOTLIN_1_3, kind = BUG_FIX),
    ProhibitAssigningSingleElementsToVarargsInNamedForm(KOTLIN_1_3, kind = BUG_FIX),
    FunctionTypesWithBigArity(KOTLIN_1_3, sinceApiVersion = ApiVersion.KOTLIN_1_3),
    RestrictRetentionForExpressionAnnotations(KOTLIN_1_3, kind = BUG_FIX),
    NormalizeConstructorCalls(KOTLIN_1_3),
    StrictJavaNullabilityAssertions(KOTLIN_1_3, kind = BUG_FIX),
    SoundSmartcastForEnumEntries(KOTLIN_1_3, kind = BUG_FIX),
    ProhibitErroneousExpressionsInAnnotationsWithUseSiteTargets(KOTLIN_1_3, kind = BUG_FIX),
    NewCapturedReceiverFieldNamingConvention(KOTLIN_1_3, kind = BUG_FIX),
    ExtendedMainConvention(KOTLIN_1_3),
    ExperimentalBuilderInference(KOTLIN_1_3),

    DslMarkerOnFunctionTypeReceiver(KOTLIN_1_4, kind = BUG_FIX),
    RestrictReturnStatementTarget(KOTLIN_1_4, kind = BUG_FIX),
    NoConstantValueAttributeForNonConstVals(KOTLIN_1_4, kind = BUG_FIX),
    WarningOnMainUnusedParameter(KOTLIN_1_4),
    PolymorphicSignature(KOTLIN_1_4),
    ProhibitConcurrentHashMapContains(KOTLIN_1_4, kind = BUG_FIX),
    ProhibitTypeParametersForLocalVariables(KOTLIN_1_4, kind = BUG_FIX),
    ProhibitJvmOverloadsOnConstructorsOfAnnotationClasses(KOTLIN_1_4, kind = BUG_FIX),

    ProperVisibilityForCompanionObjectInstanceField(sinceVersion = null, kind = BUG_FIX),
    // Temporarily disabled, see KT-27084/KT-22379
    SoundSmartcastFromLoopConditionForLoopAssignedVariables(sinceVersion = null, kind = BUG_FIX),

    ProperIeee754Comparisons(sinceVersion = null, defaultState = State.DISABLED, kind = BUG_FIX),

    // Experimental features

    Coroutines(
        KOTLIN_1_1, ApiVersion.KOTLIN_1_1,
        "https://kotlinlang.org/docs/diagnostics/experimental-coroutines",
        State.ENABLED_WITH_WARNING
    ),

    MultiPlatformProjects(sinceVersion = null, defaultState = State.DISABLED),

    NewInference(sinceVersion = KOTLIN_1_3, defaultState = State.DISABLED),
    // This feature can be enabled only along with new inference, see KT-26357 for details
    BooleanElvisBoundSmartCasts(sinceVersion = KOTLIN_1_3, defaultState = State.DISABLED),

    SamConversionForKotlinFunctions(sinceVersion = KOTLIN_1_3, defaultState = State.DISABLED),

    InlineClasses(sinceVersion = KOTLIN_1_3, defaultState = State.ENABLED_WITH_WARNING, kind = UNSTABLE_FEATURE),

    ;

    val presentableName: String
    // E.g. "DestructuringLambdaParameters" -> ["Destructuring", "Lambda", "Parameters"] -> "destructuring lambda parameters"
        get() = name.split("(?<!^)(?=[A-Z])".toRegex()).joinToString(separator = " ", transform = String::toLowerCase)

    val presentableText get() = if (hintUrl == null) presentableName else "$presentableName (See: $hintUrl)"

    enum class State(override val description: String) : DescriptionAware {
        ENABLED("Enabled"),
        ENABLED_WITH_WARNING("Enabled with warning"),
        ENABLED_WITH_ERROR("Disabled"), // TODO: consider dropping this and using DISABLED instead
        DISABLED("Disabled");
    }

    /**
     * # [forcesPreReleaseBinaries]
     * If 'true', then enabling this feature (e.g. by '-XXLanguage:', or dedicated '-X'-flag)
     * will force generation of pre-release binaries (given that [sinceVersion] > [LanguageVersion.LATEST_STABLE]).
     * Use it for features that involve generation of non-trivial low-level code with non-finalized design.
     *
     * Note that [forcesPreReleaseBinaries] makes sense only for features with [sinceVersion] > [LanguageVersion.LATEST_STABLE].
     *
     * Please, DO NOT use features that force pre-release binaries in the Kotlin project, as that would
     * generate 'kotlin-compiler' as pre-release.
     *
     *
     * # [enabledInProgressiveMode]
     * If 'true', then this feature will be automatically enabled under '-progressive' mode.
     *
     * Restrictions for using this flag for particular feature follow from restrictions of the progressive mode:
     * - enabling it *must not* break compatibility with non-progressive compiler, i.e. code written under progressive
     *   should compile successfully by non-progressive compiler with the same language version settings.
     *   Example: making some "red" code "green" is not fine, because non-progressive compilers won't be able to compile
     *   such code
     *
     * - changes in language semantics should not be "silent": user must receive some message from the compiler
     *   about all affected code. Exceptions are possible on case-by-case basis.
     *   Example: silently changing semantics of generated low-level code is not fine, but deprecating some language
     *   construction immediately instead of a going through complete deprecation cycle is fine.
     *
     * NB: Currently, [enabledInProgressiveMode] makes sense only for features with [sinceVersion] > [LanguageVersion.LATEST_STABLE]
     */
    enum class Kind(val enabledInProgressiveMode: Boolean, val forcesPreReleaseBinaries: Boolean) {
        /**
         * Simple bug fix which just forbids some language constructions.
         * Rule of thumb: it turns "green code" into "red".
         *
         * Note that, some actual bug fixes can affect overload resolution/inference, silently changing semantics of
         * users' code -- DO NOT use Kind.BUG_FIX for them!
         */
        BUG_FIX(true, false),

        /**
         * Enables support of some new and *unstable* construction in language.
         * Rule of thumb: it turns "red" code into "green", and we want to strongly demotivate people from manually enabling
         * that feature in production.
         */
        UNSTABLE_FEATURE(false, true),

        /**
         * A new feature in the language which has no impact on the binary output of the compiler, and therefore
         * does not cause pre-release binaries to be generated.
         * Rule of thumb: it turns "red" code into "green" and the old compilers can correctly use the binaries
         * produced by the new compiler.
         *
         * NB. OTHER is not a conservative fallback, as it doesn't imply generation of pre-release binaries
         */
        OTHER(false, false),
    }

    companion object {
        @JvmStatic
        fun fromString(str: String) = values().find { it.name == str }
    }
}

enum class LanguageVersion(val major: Int, val minor: Int) : DescriptionAware {
    KOTLIN_1_0(1, 0),
    KOTLIN_1_1(1, 1),
    KOTLIN_1_2(1, 2),
    KOTLIN_1_3(1, 3),
    KOTLIN_1_4(1, 4);

    val isStable: Boolean
        get() = this <= LATEST_STABLE

    val versionString: String
        get() = "$major.$minor"

    override val description: String
        get() = if (isStable) versionString else "$versionString (EXPERIMENTAL)"

    override fun toString() = versionString

    companion object {
        @JvmStatic
        fun fromVersionString(str: String?) = values().find { it.versionString == str }

        @JvmStatic
        fun fromFullVersionString(str: String) =
            str.split(".", "-").let { if (it.size >= 2) fromVersionString("${it[0]}.${it[1]}") else null }

        @JvmField
        val FIRST_SUPPORTED = KOTLIN_1_2

        @JvmField
        val LATEST_STABLE = KOTLIN_1_3
    }
}

interface LanguageVersionSettings {
    fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State

    fun supportsFeature(feature: LanguageFeature): Boolean =
        getFeatureSupport(feature).let {
            it == LanguageFeature.State.ENABLED ||
            it == LanguageFeature.State.ENABLED_WITH_WARNING
        }

    fun isPreRelease(): Boolean

    fun <T> getFlag(flag: AnalysisFlag<T>): T

    val apiVersion: ApiVersion

    // Please do not use this to enable/disable specific features/checks. Instead add a new LanguageFeature entry and call supportsFeature
    val languageVersion: LanguageVersion
}

class LanguageVersionSettingsImpl @JvmOverloads constructor(
    override val languageVersion: LanguageVersion,
    override val apiVersion: ApiVersion,
    analysisFlags: Map<AnalysisFlag<*>, Any?> = emptyMap(),
    specificFeatures: Map<LanguageFeature, LanguageFeature.State> = emptyMap()
) : LanguageVersionSettings {
    private val analysisFlags: Map<AnalysisFlag<*>, *> = Collections.unmodifiableMap(analysisFlags)
    private val specificFeatures: Map<LanguageFeature, LanguageFeature.State> = Collections.unmodifiableMap(specificFeatures)

    @Suppress("UNCHECKED_CAST")
    override fun <T> getFlag(flag: AnalysisFlag<T>): T = analysisFlags[flag] as T? ?: flag.defaultValue

    override fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State {
        specificFeatures[feature]?.let { return it }

        val since = feature.sinceVersion
        if (since != null && languageVersion >= since && apiVersion >= feature.sinceApiVersion) {
            return feature.defaultState
        }

        return LanguageFeature.State.DISABLED
    }

    override fun toString() = buildString {
        append("Language = $languageVersion, API = $apiVersion")
        specificFeatures.forEach { (feature, state) ->
            val char = when (state) {
                LanguageFeature.State.ENABLED -> '+'
                LanguageFeature.State.ENABLED_WITH_WARNING -> '~'
                LanguageFeature.State.ENABLED_WITH_ERROR, LanguageFeature.State.DISABLED -> '-'
            }
            append(" $char$feature")
        }
    }

    override fun isPreRelease(): Boolean = languageVersion.isPreRelease() ||
            specificFeatures.any { (feature, state) ->
                state == LanguageFeature.State.ENABLED && feature.forcesPreReleaseBinariesIfEnabled()
            }

    companion object {
        @JvmField
        val DEFAULT = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE)
    }
}

fun LanguageVersion.isPreRelease(): Boolean {
    if (!isStable) return true

    return KotlinCompilerVersion.isPreRelease() && this == LanguageVersion.LATEST_STABLE
}

fun LanguageFeature.forcesPreReleaseBinariesIfEnabled(): Boolean {
    val isFeatureNotReleasedYet = sinceVersion?.isStable != true
    return isFeatureNotReleasedYet && kind.forcesPreReleaseBinaries
}
