// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: mangling2.def
---

// test mangling of special names

enum Companion {One, Two};


// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import kotlinx.cinterop.*
import kotlin.test.*
import mangling2.*

fun box(): String {
    val mangled = `Companion$`.Two
    assertEquals(`Companion$`.Two, mangled)
    return "OK"
}

