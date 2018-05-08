/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

enum class TargetBackend(
    private val compatibleWithTargetBackend: TargetBackend? = null
) {
    ANY,
    JVM,
    JVM_IR(JVM),
    JS,
    JS_IR(JS);

    val compatibleWith get() = compatibleWithTargetBackend ?: ANY
}
