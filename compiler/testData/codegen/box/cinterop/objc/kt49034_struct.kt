/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
// TARGET_BACKEND: NATIVE
// The test depends on collision between kt49034.JSContext and platform.JavaScriptCore.JSContext
// DISABLE_NATIVE: isAppleTarget=false
//   There is no JavaScriptCore on watchOS.
// DISABLE_NATIVE: targetFamily=WATCHOS

// MODULE: cinterop
// FILE: kt49034.def
headers = kt49034.h

// FILE: kt49034.h
#ifdef __cplusplus
extern "C" {
#endif

struct JSContext;

struct JSContext* bar();

#ifdef __cplusplus
}
#endif

// FILE: impl.c
#include "kt49034.h"

struct JSContext {
    int field;
};

struct JSContext global = { 15 };

extern "C" struct JSContext* bar() {
    return &global;
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import kt49034.bar
import cnames.structs.JSContext
import kotlinx.cinterop.CPointer

fun baz(s: CPointer<JSContext>) {
    println(s)
}

fun box(): String {
    baz(bar()!!)
    return "OK"
}

// FILE: checkPlatformJavaScriptCore.kt
import platform.JavaScriptCore.JSContext
