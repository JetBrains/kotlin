// TARGET_BACKEND: NATIVE


@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(kotlin.ExperimentalStdlibApi::class)

import kotlin.native.concurrent.*
import kotlin.concurrent.*
import kotlin.native.internal.*
import kotlin.reflect.KMutableProperty0

// Overload resolution is not working in K2 with Supress INVISIBLE_REFERENCE.
// But resolving constants and annotations work
// So we are creating local copies of this intrinsic for test

@TypedIntrinsic(IntrinsicType.GET_AND_ADD_FIELD)
internal external fun KMutableProperty0<Short>.getAndAddFieldLocal(delta: Short): Short
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_FIELD)
internal external fun KMutableProperty0<Int>.getAndAddFieldLocal(newValue: Int): Int
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_FIELD)
internal external fun KMutableProperty0<Long>.getAndAddFieldLocal(newValue: Long): Long
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_FIELD)
internal external fun KMutableProperty0<Byte>.getAndAddFieldLocal(newValue: Byte): Byte


val a = "1"
val b = "2"
val c = "3"

@Volatile var x: Int = 1
@Volatile var y: Long = 1L
@Volatile var z: String = a
@Volatile var t: Boolean = true

fun box() : String {
    if (::x.compareAndSetField(1, 2) != true) return "FAIL Int: 1"
    if (::x.compareAndSetField(1, 2) != false) return "FAIL Int: 2"
    if (::x.compareAndSwapField(2, 1) != 2) return "FAIL Int: 3"
    if (::x.compareAndSwapField(2, 1) != 1) return "FAIL Int: 4"
    if (::x.getAndSetField(3) != 1) return "FAIL Int: 5"
    if (::x.getAndSetField(1) != 3) return "FAIL Int: 6"
    if (::x.getAndAddFieldLocal(1) != 1) return "FAIL Int: 7"
    if (::x.getAndAddFieldLocal(1) != 2) return "FAIL Int: 8"
    if (x != 3) return "FAIL Int: 9"

    if (::y.compareAndSetField(1L, 2L) != true) return "FAIL Long: 1"
    if (::y.compareAndSetField(1L, 2L) != false) return "FAIL Long: 2"
    if (::y.compareAndSwapField(2L, 1L) != 2L) return "FAIL Long: 3"
    if (::y.compareAndSwapField(2L, 1L) != 1L) return "FAIL Long: 4"
    if (::y.getAndSetField(3L) != 1L) return "FAIL Long: 5"
    if (::y.getAndSetField(1L) != 3L) return "FAIL Long: 6"
    if (::y.getAndAddFieldLocal(1L) != 1L) return "FAIL Long: 7"
    if (::y.getAndAddFieldLocal(1L) != 2L) return "FAIL Long: 8"
    if (y != 3L) return "FAIL Long: 9"


    if (isExperimentalMM()) {
        if (::z.compareAndSetField(a, b) != true) return "FAIL String: 1"
        if (::z.compareAndSetField(a, b) != false) return "FAIL String: 2"
        if (::z.compareAndSwapField(b, a) != b) return "FAIL String: 3"
        if (::z.compareAndSwapField(b, a) != a) return "FAIL String: 4"
        if (::z.getAndSetField(c) != a) return "FAIL String: 5"
        if (::z.getAndSetField(a) != c) return "FAIL String: 6"
        if (z != a) return "FAIL String: 7"
    }

    if (::t.compareAndSetField(true, false) != true) return "FAIL Bool: 1"
    if (::t.compareAndSetField(true, false) != false) return "FAIL Bool: 2"
    if (::t.compareAndSwapField(false, true) != false) return "FAIL Bool: 3"
    if (::t.compareAndSwapField(false, true) != true) return "FAIL Bool: 4"
    if (::t.getAndSetField(false) != true) return "FAIL Bool: 5"
    if (::t.getAndSetField(true) != false) return "FAIL Bool: 6"
    if (t != true) return "FAIL Bool: 7"

    return "OK"
}
