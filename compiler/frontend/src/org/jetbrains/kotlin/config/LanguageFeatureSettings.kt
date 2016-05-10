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

class LanguageFeatureSettings(
        val typeAliases: Boolean = true,
        val localDelegatedProperties: Boolean = true,
        val topLevelSealedInheritance: Boolean = true
) {
    companion object {
        private val SETTINGS = mapOf(
                "1.0" to LanguageFeatureSettings(
                        typeAliases = false,
                        localDelegatedProperties = false,
                        topLevelSealedInheritance = false
                ),
                "1.1" to LanguageFeatureSettings()
        )

        private val LATEST_VERSION = "1.1"

        @JvmField
        val LATEST: LanguageFeatureSettings = fromLanguageVersion(LATEST_VERSION)!!

        @JvmStatic
        fun fromLanguageVersion(source: String): LanguageFeatureSettings? = SETTINGS[source]
    }
}
