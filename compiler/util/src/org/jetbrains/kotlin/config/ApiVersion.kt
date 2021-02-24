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

import org.jetbrains.kotlin.utils.DescriptionAware

class ApiVersion private constructor(
        val version: MavenComparableVersion,
        val versionString: String
) : Comparable<ApiVersion>, DescriptionAware {
    val isStable: Boolean
        get() = this <= ApiVersion.LATEST_STABLE

    override val description: String
        get() = if (isStable) versionString else "$versionString (EXPERIMENTAL)"

    override fun compareTo(other: ApiVersion): Int =
            version.compareTo(other.version)

    override fun equals(other: Any?) =
            (other as? ApiVersion)?.version == version

    override fun hashCode() =
            version.hashCode()

    override fun toString() = versionString

    companion object {
        @JvmField
        val KOTLIN_1_0 = createByLanguageVersion(LanguageVersion.KOTLIN_1_0)

        @JvmField
        val KOTLIN_1_1 = createByLanguageVersion(LanguageVersion.KOTLIN_1_1)

        @JvmField
        val KOTLIN_1_2 = createByLanguageVersion(LanguageVersion.KOTLIN_1_2)

        @JvmField
        val KOTLIN_1_3 = createByLanguageVersion(LanguageVersion.KOTLIN_1_3)

        @JvmField
        val KOTLIN_1_4 = createByLanguageVersion(LanguageVersion.KOTLIN_1_4)

        @JvmField
        val KOTLIN_1_5 = createByLanguageVersion(LanguageVersion.KOTLIN_1_5)

        @JvmField
        val LATEST: ApiVersion = createByLanguageVersion(LanguageVersion.values().last())

        @JvmField
        val LATEST_STABLE: ApiVersion = createByLanguageVersion(LanguageVersion.LATEST_STABLE)

        @JvmStatic
        fun createByLanguageVersion(version: LanguageVersion): ApiVersion = parse(version.versionString)!!

        fun parse(versionString: String): ApiVersion? = try {
            ApiVersion(MavenComparableVersion(versionString), versionString)
        }
        catch (e: Exception) {
            null
        }
    }
}
