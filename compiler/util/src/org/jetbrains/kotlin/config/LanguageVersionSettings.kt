/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.config.LanguageVersion.*
import org.jetbrains.kotlin.utils.DescriptionAware
import java.util.*

enum class LanguageFeature(
        val sinceVersion: LanguageVersion?,
        val sinceApiVersion: ApiVersion = ApiVersion.KOTLIN_1_0,
        val hintUrl: String? = null,
        val defaultState: State = State.ENABLED
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

    BooleanElvisBoundSmartCasts(KOTLIN_1_3),
    ReturnsEffect(KOTLIN_1_3),
    CallsInPlaceEffect(KOTLIN_1_3),
    RestrictionOfValReassignmentViaBackingField(KOTLIN_1_3),
    NestedClassesInEnumEntryShouldBeInner(KOTLIN_1_3),
    ProhibitDataClassesOverridingCopy(KOTLIN_1_3),
    RestrictionOfWrongAnnotationsWithUseSiteTargetsOnTypes(KOTLIN_1_3),
    ProhibitInnerClassesOfGenericClassExtendingThrowable(KOTLIN_1_3),

    StrictJavaNullabilityAssertions(sinceVersion = null, defaultState = State.DISABLED),

    // Experimental features

    Coroutines(KOTLIN_1_1, ApiVersion.KOTLIN_1_1, "https://kotlinlang.org/docs/diagnostics/experimental-coroutines", State.ENABLED_WITH_WARNING),

    MultiPlatformProjects(sinceVersion = null, defaultState = State.DISABLED),

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

    companion object {
        @JvmStatic
        fun fromString(str: String) = values().find { it.name == str }
    }
}

enum class LanguageVersion(val major: Int, val minor: Int) : DescriptionAware {
    KOTLIN_1_0(1, 0),
    KOTLIN_1_1(1, 1),
    KOTLIN_1_2(1, 2),
    KOTLIN_1_3(1, 3);

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
        fun fromFullVersionString(str: String) = str.split(".", "-").let { if (it.size >= 2) fromVersionString("${it[0]}.${it[1]}") else null }

        @JvmField
        val LATEST_STABLE = KOTLIN_1_2
    }
}

interface LanguageVersionSettings {
    fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State

    fun supportsFeature(feature: LanguageFeature): Boolean =
            getFeatureSupport(feature).let { it == LanguageFeature.State.ENABLED || it == LanguageFeature.State.ENABLED_WITH_WARNING }

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

    companion object {
        @JvmField
        val DEFAULT = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE)
    }
}

fun LanguageVersionSettings.shouldWritePreReleaseFlag(): Boolean =
        languageVersion.shouldWritePreReleaseFlag()

fun LanguageVersion.shouldWritePreReleaseFlag(): Boolean {
    if (!isStable) return true

    return KotlinCompilerVersion.isPreRelease() && this == LanguageVersion.LATEST_STABLE
}
