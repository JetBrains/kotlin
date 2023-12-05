// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: carrayPointers.def
---
int (*arrayPointer)[1];

int globalArray[3] = {1, 2, 3};

struct StructWithArrayPtr {
    int (*arrayPointer)[1];
};

// MODULE: main(cinterop)
// FILE: main.kt

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import carrayPointers.*
import kotlin.test.*
import kotlinx.cinterop.*

fun box(): String {
    arrayPointer = globalArray
    assertEquals(globalArray[0], arrayPointer!![0])
    arrayPointer!![0] = 15
    assertEquals(15, globalArray[0])

    memScoped {
        val struct = alloc<StructWithArrayPtr>()
        struct.arrayPointer = globalArray
        assertEquals(globalArray[0], struct.arrayPointer!![0])
    }
    return "OK"
}
