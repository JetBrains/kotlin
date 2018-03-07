/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import java.io.File

val JS_IR_BACKEND_TEST_WHITELIST = listOf(
    "js/js.translator/testData/box/annotation"
).map { File(it) }
