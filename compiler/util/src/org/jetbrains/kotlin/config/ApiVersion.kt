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

class ApiVersion private constructor(
        private val version: MavenComparableVersion,
        val versionString: String
) : Comparable<ApiVersion> {
    override fun compareTo(other: ApiVersion): Int =
            version.compareTo(other.version)

    override fun equals(other: Any?) =
            (other as? ApiVersion)?.version == version

    override fun hashCode() =
            version.hashCode()

    override fun toString() = versionString

    companion object {
        @JvmField
        val LATEST: ApiVersion = createByLanguageVersion(LanguageVersion.Companion.LATEST)

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
