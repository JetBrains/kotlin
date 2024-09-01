/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// KT-66106: uncaught exception: Wrong box result 'undefined'; Expected "OK"
// IGNORE_BACKEND: WASM
// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    main(arrayOf("OK"))
    return sb.toString()
}

fun main(args : Array<String>) {
    run {
        sb.append(args[0])
    }
}

fun run(f: () -> Unit) {
    f()
}