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

import org.jetbrains.kotlin.serialization.deserialization.descriptors.SinceKotlinInfo

class ApiVersion private constructor(
        val version: MavenComparableVersion,
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
        val KOTLIN_1_0 = createByLanguageVersion(LanguageVersion.KOTLIN_1_0)

        @JvmField
        val KOTLIN_1_1 = createByLanguageVersion(LanguageVersion.KOTLIN_1_1)

        @JvmField
        val LATEST: ApiVersion = createByLanguageVersion(LanguageVersion.LATEST)

        @JvmStatic
        fun createByLanguageVersion(version: LanguageVersion): ApiVersion = parse(version.versionString)!!

        @JvmStatic
        fun createBySinceKotlinInfo(sinceKotlinInfo: SinceKotlinInfo): ApiVersion =
                sinceKotlinInfo.version.let { version -> parse(version.asString()) ?: error("Could not parse version: $version") }

        fun parse(versionString: String): ApiVersion? = try {
            ApiVersion(MavenComparableVersion(versionString), versionString)
        }
        catch (e: Exception) {
            null
        }
    }
}
