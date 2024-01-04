// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: cvectors.def
---
typedef float __attribute__ ((__vector_size__ (16)))   KVector4f;
typedef int   __attribute__ ((__vector_size__ (16)))   KVector4i32;

struct Complex {
    unsigned int ui;
    KVector4f vec4f;
    struct Complex* next;
    int arr[2];
};

struct Complex produceComplexVector() {
    struct Complex complex = {
        .ui = 128,
        .vec4f = {1.0, 1.0, 1.0, 1.0},
        .next = 0,
        .arr = {-51, -19}
    };
    return complex;
};

static float sendV4F(KVector4f v) {
    return v[0] + 2 * v[1] + 4 * v[2] + 8 * v[3];
}

static int sendV4I(KVector4i32 v) {
    return v[0] + 2 * v[1] + 4 * v[2] + 8 * v[3];
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)

import kotlinx.cinterop.*
import kotlinx.cinterop.vectorOf
import kotlin.native.*
import kotlin.test.*
import cvectors.*

fun box(): String {
    produceComplexVector().useContents {
        assertEquals(vec4f, vectorOf(1.0f, 1.0f, 1.0f, 1.0f))
        vec4f = vectorOf(0.0f, 0.0f, 0.0f, 0.0f)
        assertEquals(vec4f, vectorOf(0.0f, 0.0f, 0.0f, 0.0f))
    }

    // FIXME: KT-36285
    if (Platform.osFamily != OsFamily.LINUX || Platform.cpuArchitecture != CpuArchitecture.ARM32) {
        assertEquals(49, sendV4I(vectorOf(1, 2, 3, 4)))
    }
    assertEquals(49, (sendV4F(vectorOf(1f, 2f, 3f, 4f)) + 0.00001).toInt())

    memScoped {
        val vector = alloc<KVector4i32Var>().also {
            it.value = vectorOf(1, 2, 3, 4)
        }
        assertEquals(vector.value, vectorOf(1, 2, 3, 4))
    }

    return "OK"
}
