/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import java.io.File

enum class TargetBackend(
    // if whitelist === null it will not be used in test generator (will work as usual)
    // if path to testData file starts with or equals to any entry from whitelist it will be processed as usual
    // otherwise a testData file will be treated as ignored
    val whitelist: List<File>? = null
) {
    ANY,
    JVM,
    JVM_IR,
    JS,
    JS_IR(JS_IR_BACKEND_TEST_WHITELIST);
}
