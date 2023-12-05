/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// May need disabling when gcSchedulerType=aggressive . since it may be too slow

// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: bitfields.def
---
enum B2 {
    ZERO, ONE, TWO, THREE
};

enum E : short {
    A, B
};

struct __attribute__((packed)) S {
    long long x1 : 1;
    enum B2 x2 : 2;
    unsigned short x3 : 3;
    unsigned int x4 : 4;
    int x5 : 5;
    long long x6 : 63;
    enum E x7: 2;
    _Bool x8 : 1;
    struct { int x9:4; };
};

static long long getX1(struct S* s) { return s->x1; }
static enum B2 getX2(struct S* s) { return s->x2; }
static unsigned short getX3(struct S* s) { return s->x3; }
static unsigned int getX4(struct S* s) { return s->x4; }
static int getX5(struct S* s) { return s->x5; }
static long long getX6(struct S* s) { return s->x6; }
static enum E getX7(struct S* s) { return s->x7; }
static _Bool getX8(struct S* s) { return s->x8; }
static int getX9(struct S* s) { return s->x9; }

// MODULE: main(cinterop)
// FILE: main.kt

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import bitfields.*
import kotlinx.cinterop.*

fun assertEquals(value1: Any?, value2: Any?) {
    if (value1 != value2)
        throw AssertionError("Expected $value1, got $value2")
}

fun check(s: S, x1: Long, x2: B2, x3: UShort, x4: UInt, x5: Int, x6: Long, x7: E, x8: Boolean, x9: Int) {
    assertEquals(x1, s.x1)
    assertEquals(x1, getX1(s.ptr))

    assertEquals(x2, s.x2)
    assertEquals(x2, getX2(s.ptr))

    assertEquals(x3, s.x3)
    assertEquals(x3, getX3(s.ptr))

    assertEquals(x4, s.x4)
    assertEquals(x4, getX4(s.ptr))

    assertEquals(x5, s.x5)
    assertEquals(x5, getX5(s.ptr))

    assertEquals(x6, s.x6)
    assertEquals(x6, getX6(s.ptr))

    assertEquals(x7, s.x7)
    assertEquals(x7, getX7(s.ptr))

    assertEquals(x8, s.x8)
    assertEquals(x8, getX8(s.ptr))

    assertEquals(x9, s.x9)
    assertEquals(x9, getX9(s.ptr))
}

fun assign(s: S, x1: Long, x2: B2, x3: UShort, x4: UInt, x5: Int, x6: Long, x7: E, x8: Boolean, x9: Int) {
    s.x1 = x1
    s.x2 = x2
    s.x3 = x3
    s.x4 = x4
    s.x5 = x5
    s.x6 = x6
    s.x7 = x7
    s.x8 = x8
    s.x9 = x9
}

fun assignReversed(s: S, x1: Long, x2: B2, x3: UShort, x4: UInt, x5: Int, x6: Long, x7: E, x8: Boolean, x9: Int) {
    s.x9 = x9
    s.x8 = x8
    s.x7 = x7
    s.x6 = x6
    s.x5 = x5
    s.x4 = x4
    s.x3 = x3
    s.x2 = x2
    s.x1 = x1
}

fun test(s: S, x1: Long, x2: B2, x3: UShort, x4: UInt, x5: Int, x6: Long, x7: E, x8: Boolean, x9: Int) {
    assign(s, x1, x2, x3, x4, x5, x6, x7, x8, x9)
    check(s, x1, x2, x3, x4, x5, x6, x7, x8, x9)

    assignReversed(s, x1, x2, x3, x4, x5, x6, x7, x8, x9)
    check(s, x1, x2, x3, x4, x5, x6, x7, x8, x9)

    // Also check with some insignificant bits modified:

    assign(s, x1 + 2, x2, (x3 + 8u).toUShort(), x4 - 16u, x5 + 32, x6 + Long.MIN_VALUE, x7, x8, x9 + 16)
    check(s, x1, x2, x3, x4, x5, x6, x7, x8, x9)

    assignReversed(s, x1 + 2, x2, (x3 + 8u).toUShort(), x4 - 16u, x5 + 32, x6 + Long.MIN_VALUE, x7, x8, x9 + 16)
    check(s, x1, x2, x3, x4, x5, x6, x7, x8, x9)
}

fun box(): String {
    memScoped {
        val s = alloc<S>()
        for (x1 in -1L..0L)
            for (x2 in arrayOf(B2.ZERO, B2.ONE, B2.THREE))
                for (x3 in ushortArrayOf(0u, 2u, 7u))
                    for (x4 in uintArrayOf(0u, 6u, 15u))
                        for (x5 in intArrayOf(-16, -1, 0, 5, 15))
                            for (x6 in longArrayOf(Long.MIN_VALUE/2, -325L, 0, 1L shl 48, Long.MAX_VALUE/2))
                                for (x7 in E.values())
                                    for (x8 in arrayOf(false, true))
                                        for (x9 in intArrayOf(-8, -1, 0, 5, 7)) // 4 bits width
                                            test(s, x1, x2, x3.toUShort(), x4, x5, x6, x7, x8, x9)
    }
    return "OK"
}
