/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

data class KotlinTestInfo(
    val className: String,
    val methodName: String,
    val tags: Set<String>
) : TestService

val TestServices.testInfo: KotlinTestInfo by TestServices.testServiceAccessor()
