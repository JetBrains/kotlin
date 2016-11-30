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

import org.jetbrains.kotlin.utils.DescriptionAware

enum class LanguageFeature(val sinceVersion: LanguageVersion) {
    ;
    companion object {
        @JvmStatic
        fun fromString(str: String) = values().find { it.name == str }
    }
}

enum class LanguageVersion(val versionString: String) : DescriptionAware {
    KOTLIN_1_0("1.0");

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

    val languageVersion: LanguageVersion
    val apiVersion: ApiVersion
}

class LanguageVersionSettingsImpl(
        override val languageVersion: LanguageVersion,
        override val apiVersion: ApiVersion
) : LanguageVersionSettings {
    override fun supportsFeature(feature: LanguageFeature): Boolean {
        return languageVersion >= feature.sinceVersion
    }

    override fun toString() = "Language = $languageVersion, API = $apiVersion"

    companion object {
        @JvmField
        val DEFAULT = LanguageVersionSettingsImpl(LanguageVersion.LATEST, ApiVersion.LATEST)
    }
}
