/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.base

enum class KotlinReleaseVersion(
    val releaseName: String,
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<KotlinReleaseVersion> {
    v1_0_0("1.0.0", 1, 0, 0),
    v1_4_0("1.4.0", 1, 4, 0),
    v1_9_20("1.9.20", 1, 9, 20),
    v2_0_0("2.0.0", 2, 0, 0),
    v2_0_20("2.0.20", 2, 0, 20),
    v2_1_0("2.1.0", 2, 1, 0),
    v2_1_20("2.1.20", 2 ,1, 20),
    v2_2_0("2.2.0", 2, 2, 0),
}
