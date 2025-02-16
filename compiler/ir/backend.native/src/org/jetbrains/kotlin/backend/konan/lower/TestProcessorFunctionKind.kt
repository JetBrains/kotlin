/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.name.FqName

enum class TestProcessorFunctionKind(annotationNameString: String, val runtimeKindString: String) {
    TEST("kotlin.test.Test", ""),
    BEFORE_TEST("kotlin.test.BeforeTest", "BEFORE_TEST"),
    AFTER_TEST("kotlin.test.AfterTest", "AFTER_TEST"),
    BEFORE_CLASS("kotlin.test.BeforeClass", "BEFORE_CLASS"),
    AFTER_CLASS("kotlin.test.AfterClass", "AFTER_CLASS");

    val annotationFqName = FqName(annotationNameString)

    companion object {
        val INSTANCE_KINDS = listOf(TEST, BEFORE_TEST, AFTER_TEST)
        val COMPANION_KINDS = listOf(BEFORE_CLASS, AFTER_CLASS)
    }
}
