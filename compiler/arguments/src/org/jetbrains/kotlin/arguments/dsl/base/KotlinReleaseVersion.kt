/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.base

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.serialization.json.KotlinReleaseVersionAsNameSerializer

/**
 * Defines all Kotlin release versions.
 *
 * @param releaseName human-readable release name. Usually in the "< major>.<minor>.<patch >" format.
 * @param major major number in the given release version
 * @param minor minor number in the given release version
 * @param patch patch number in the given release version
 *
 * @see [WithKotlinReleaseVersionsMetadata]
 */
@Suppress("EnumEntryName")
@Serializable(with = KotlinReleaseVersionAsNameSerializer::class)
enum class KotlinReleaseVersion(
    val releaseName: String,
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<KotlinReleaseVersion> {
    v1_0_0("1.0.0", 1, 0, 0),
    v1_0_7("1.0.7", 1, 0, 7),
    v1_1_0("1.1.0", 1, 1, 0),
    v1_1_2("1.1.2", 1, 1, 2),
    v1_1_50("1.1.50", 1, 1, 50),
    v1_1_61("1.1.61", 1, 1, 61),
    v1_2_0("1.2.0", 1, 2, 0),
    v1_2_71("1.2.71", 1, 2, 71),
    v1_3_0("1.3.0", 1, 3, 0),
    v1_3_30("1.3.30", 1, 3, 30),
    v1_3_70("1.3.70", 1, 3, 70),
    v1_3_72("1.3.72", 1, 3, 72),
    v1_4_0("1.4.0", 1, 4, 0),
    v1_4_20("1.4.20", 1, 4, 20),
    v1_4_32("1.4.32", 1, 4, 32),
    v1_5_0("1.5.0", 1, 5, 0),
    v1_5_32("1.5.32", 1, 5, 32),
    v1_6_0("1.6.0", 1, 6, 0),
    v1_6_20("1.6.20", 1, 6, 20),
    v1_6_21("1.6.21", 1, 6, 21),
    v1_7_0("1.7.0", 1, 7, 0),
    v1_7_21("1.7.21", 1, 7, 21),
    v1_8_0("1.8.0", 1, 8, 0),
    v1_8_22("1.8.22", 1, 8, 22),
    v1_9_0("1.9.0", 1, 9, 0),
    v1_9_20("1.9.20", 1, 9, 20),
    v1_9_25("1.9.25", 1, 9, 25),
    v2_0_0("2.0.0", 2, 0, 0),
    v2_0_20("2.0.20", 2, 0, 20),
    v2_0_21("2.0.21", 2, 0, 21),
    v2_1_0("2.1.0", 2, 1, 0),
    v2_1_20("2.1.20", 2 ,1, 20),
    v2_1_21("2.1.21", 2 ,1, 21),
    v2_2_0("2.2.0", 2, 2, 0),
    v2_2_20("2.2.20", 2, 2, 20),
    v2_3_0("2.3.0", 2, 3, 0),
}
