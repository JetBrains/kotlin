/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: cunsupported.def
compilerOpts = -mno-xsave

---

static void noAttr() {}
__attribute__((always_inline)) noTargetAttr() {}

__attribute__((always_inline, __target__("xsave"))) void plainAttrs1() {}
__attribute__((__target__("xsave"), always_inline)) void plainAttrs2() {}

#define TARGET __target__

__attribute__((always_inline, TARGET("xsave"))) void macroAttr1() {}
__attribute__((TARGET("xsave"), always_inline)) void macroAttr2() {}

#define TARGET_ATTR __target__("xsave")

__attribute__((TARGET_ATTR, always_inline)) void macroAttr3() {}
__attribute__((always_inline, TARGET_ATTR)) void macroAttr4() {}

#define ALL_ATTRS1 __attribute__((always_inline, __target__("xsave")))

ALL_ATTRS1 void macroAttr5() {}

#define ALL_ATTRS2 __attribute__((always_inline, __target__("xsave")))

ALL_ATTRS2 void macroAttr6() {}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import kotlin.test.*
import cunsupported.*

fun box(): String {
    noAttr()
    noTargetAttr()

    return "OK"
}
