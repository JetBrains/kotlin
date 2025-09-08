/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    mainFun(arrayOf("OK"))
    return sb.toString()
}

fun mainFun(args : Array<String>) {
    run {
        sb.append(args[0])
    }
}

fun run(f: () -> Unit) {
    f()
}