/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.utils.DescriptionAware

class ApiVersion private constructor(
        val version: MavenComparableVersion,
        override val versionString: String
) : Comparable<ApiVersion>, DescriptionAware, LanguageOrApiVersion {

    override val isStable: Boolean
        get() = this <= LATEST_STABLE

    override val isDeprecated: Boolean
        get() = FIRST_SUPPORTED <= this && this < FIRST_NON_DEPRECATED

    override val isUnsupported: Boolean
        get() = this < FIRST_SUPPORTED

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
        val KOTLIN_1_6 = createByLanguageVersion(LanguageVersion.KOTLIN_1_6)

        @JvmField
        val KOTLIN_1_7 = createByLanguageVersion(LanguageVersion.KOTLIN_1_7)

        @JvmField
        val KOTLIN_1_8 = createByLanguageVersion(LanguageVersion.KOTLIN_1_8)

        @JvmField
        val KOTLIN_1_9 = createByLanguageVersion(LanguageVersion.KOTLIN_1_9)

        @JvmField
        val KOTLIN_2_0 = createByLanguageVersion(LanguageVersion.KOTLIN_2_0)

        @JvmField
        val KOTLIN_2_1 = createByLanguageVersion(LanguageVersion.KOTLIN_2_1)

        @JvmField
        val LATEST: ApiVersion = createByLanguageVersion(LanguageVersion.values().last())

        @JvmField
        val LATEST_STABLE: ApiVersion = createByLanguageVersion(LanguageVersion.LATEST_STABLE)

        @JvmField
        val FIRST_SUPPORTED: ApiVersion = createByLanguageVersion(LanguageVersion.FIRST_API_SUPPORTED)

        @JvmField
        val FIRST_NON_DEPRECATED: ApiVersion = createByLanguageVersion(LanguageVersion.FIRST_NON_DEPRECATED)

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
