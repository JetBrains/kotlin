/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
// TARGET_BACKEND: NATIVE
// `import objcnames` somehow works only with NATIVE_STANDALONE test directive
// NATIVE_STANDALONE
// The test checks no collision between kt49034.__darwin_fp_control and platform.posix.__darwin_fp_control
// The test makes sense only on Apple x64 targets, where `class __darwin_fp_control` present in posix platform lib
// DISABLE_NATIVE: isAppleTarget=false
// DISABLE_NATIVE: targetArchitecture=ARM64

// MODULE: cinterop
// FILE: kt49034.def
headers = kt49034.h
language = Objective-C

// FILE: kt49034.h
@class __darwin_fp_control;
void foo(__darwin_fp_control*);

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import objcnames.classes.__darwin_fp_control

open class C<T : kotlinx.cinterop.ObjCObject>

class D : C<__darwin_fp_control>()

fun box(): String {
    println(D())
    return "OK"
}

// FILE: checkPlatformDarwinFPControl.kt
import platform.posix.__darwin_fp_control
