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

enum class LanguageFeature(val sinceVersion: LanguageVersion) {
    // Note: names of these entries are also used in diagnostic tests
    TypeAliases(KOTLIN_1_1),
    BoundCallableReferences(KOTLIN_1_1),
    LocalDelegatedProperties(KOTLIN_1_1),
    TopLevelSealedInheritance(KOTLIN_1_1),
    Coroutines(KOTLIN_1_1),
    AdditionalBuiltInsMembers(KOTLIN_1_1),
    DataClassInheritance(KOTLIN_1_1),
    ;

    companion object {
        @JvmStatic
        fun fromString(str: String) = values().find { it.name == str }
    }
}

enum class LanguageVersion(val versionString: String): LanguageFeatureSettings {
    KOTLIN_1_0("1.0"),
    KOTLIN_1_1("1.1");

    override fun supportsFeature(feature: LanguageFeature): Boolean {
        return this.ordinal >= feature.sinceVersion.ordinal
    }

    companion object {
        @JvmStatic
        fun fromVersionString(str: String) = values().find { it.versionString == str }

        @JvmField
        val LATEST = values().last()
    }
}

interface LanguageFeatureSettings {
    fun supportsFeature(feature: LanguageFeature): Boolean
}
