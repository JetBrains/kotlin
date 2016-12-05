/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.config.LanguageVersion.KOTLIN_1_1
import org.jetbrains.kotlin.utils.DescriptionAware

enum class LanguageFeature(val sinceVersion: LanguageVersion?) {
    // Note: names of these entries are also used in diagnostic tests and in user-visible messages (see presentableText below)
    TypeAliases(KOTLIN_1_1),
    BoundCallableReferences(KOTLIN_1_1),
    LocalDelegatedProperties(KOTLIN_1_1),
    TopLevelSealedInheritance(KOTLIN_1_1),
    Coroutines(KOTLIN_1_1),
    AdditionalBuiltInsMembers(KOTLIN_1_1),
    DataClassInheritance(KOTLIN_1_1),
    InlineProperties(KOTLIN_1_1),
    DestructuringLambdaParameters(KOTLIN_1_1),
    SingleUnderscoreForParameterName(KOTLIN_1_1),
    DslMarkersSupport(KOTLIN_1_1),
    UnderscoresInNumericLiterals(KOTLIN_1_1),
    DivisionByZeroInConstantExpressions(KOTLIN_1_1),
    InlineConstVals(KOTLIN_1_1),

    // Experimental features
    MultiPlatformProjects(null),
    MultiPlatformDoNotCheckImpl(null),
    ;

    val presentableText: String
        // E.g. "DestructuringLambdaParameters" -> ["Destructuring", "Lambda", "Parameters"] -> "destructuring lambda parameters"
        get() = name.split("(?<!^)(?=[A-Z])".toRegex()).joinToString(separator = " ", transform = String::toLowerCase)

    companion object {
        @JvmStatic
        fun fromString(str: String) = values().find { it.name == str }
    }
}

enum class LanguageVersion(val versionString: String) : DescriptionAware {
    KOTLIN_1_0("1.0"),
    KOTLIN_1_1("1.1");

    override val description: String
        get() = versionString

    override fun toString() = versionString

    companion object {
        @JvmStatic
        fun fromVersionString(str: String) = values().find { it.versionString == str }

        @JvmStatic
        fun fromFullVersionString(str: String) = str.split(".", "-").let { if (it.size >= 2) fromVersionString("${it[0]}.${it[1]}") else null }

        @JvmField
        val LATEST = values().last()
    }
}

interface LanguageVersionSettings {
    fun supportsFeature(feature: LanguageFeature): Boolean

    val apiVersion: ApiVersion
}

class LanguageVersionSettingsImpl @JvmOverloads constructor(
        private val languageVersion: LanguageVersion,
        override val apiVersion: ApiVersion,
        additionalFeatures: Collection<LanguageFeature> = emptySet()
) : LanguageVersionSettings {
    private val additionalFeatures = additionalFeatures.toSet()

    override fun supportsFeature(feature: LanguageFeature): Boolean {
        val since = feature.sinceVersion
        return (since != null && languageVersion >= since) || feature in additionalFeatures
    }

    override fun toString() = buildString {
        append("Language = $languageVersion, API = $apiVersion")
        additionalFeatures.forEach { feature -> append(" +$feature") }
    }

    companion object {
        @JvmField
        val DEFAULT = LanguageVersionSettingsImpl(LanguageVersion.LATEST, ApiVersion.LATEST)
    }
}
