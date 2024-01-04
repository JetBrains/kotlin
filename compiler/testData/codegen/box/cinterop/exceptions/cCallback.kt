// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: cCallback.def
language = C
---
extern char* sb;
void runAndCatch(void(*f)(void));

// FILE: cCallback.cpp
#include <stdio.h>

char* sb = nullptr;
extern "C" void runAndCatch(void(*f)(void)) {
    try {
        f();
    } catch (...) {
        sb = "OK";
    }
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import cCallback.runAndCatch
import cCallback.sb

fun throwingCallback() {
    throw IllegalStateException("Kotlin Exception!")
}

fun box(): String {
    runAndCatch(staticCFunction(::throwingCallback))
    return sb?.toKString() ?: "FAIL"
}
