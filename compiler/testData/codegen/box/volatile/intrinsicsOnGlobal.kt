// TARGET_BACKEND: NATIVE
// DISABLE_IR_VISIBILITY_CHECKS: ANY

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

@Volatile var byte: Byte = 1
@Volatile var short: Short = 1
@Volatile var x: Int = 1
@Volatile var y: Long = 1L
@Volatile var z: String = a
@Volatile var t: Boolean = true

fun box() : String {
    if (::byte.compareAndSetField(1.toByte(), 2.toByte()) != true) return "FAIL Byte: 1"
    if (::byte.compareAndSetField(1.toByte(), 2.toByte()) != false) return "FAIL Byte: 2"
    if (::byte.compareAndExchangeField(2.toByte(), 1.toByte()) != 2.toByte()) return "FAIL Byte: 3"
    if (::byte.compareAndExchangeField(2.toByte(), 1.toByte()) != 1.toByte()) return "FAIL Byte: 4"
    if (::byte.getAndSetField(3.toByte()) != 1.toByte()) return "FAIL Byte: 5"
    if (::byte.getAndSetField(1.toByte()) != 3.toByte()) return "FAIL Byte: 6"
    if (::byte.getAndAddFieldLocal(1.toByte()) != 1.toByte()) return "FAIL Byte: 7"
    if (::byte.getAndAddFieldLocal(1.toByte()) != 2.toByte()) return "FAIL Byte: 8"
    if (byte != 3.toByte()) return "FAIL Byte: 9"

    if (::short.compareAndSetField(1.toShort(), 2.toShort()) != true) return "FAIL Short: 1"
    if (::short.compareAndSetField(1.toShort(), 2.toShort()) != false) return "FAIL Short: 2"
    if (::short.compareAndExchangeField(2.toShort(), 1.toShort()) != 2.toShort()) return "FAIL Short: 3"
    if (::short.compareAndExchangeField(2.toShort(), 1.toShort()) != 1.toShort()) return "FAIL Short: 4"
    if (::short.getAndSetField(3.toShort()) != 1.toShort()) return "FAIL Short: 5"
    if (::short.getAndSetField(1.toShort()) != 3.toShort()) return "FAIL Short: 6"
    if (::short.getAndAddFieldLocal(1.toShort()) != 1.toShort()) return "FAIL Short: 7"
    if (::short.getAndAddFieldLocal(1.toShort()) != 2.toShort()) return "FAIL Short: 8"
    if (short != 3.toShort()) return "FAIL Short: 9"

    if (::x.compareAndSetField(1, 2) != true) return "FAIL Int: 1"
    if (::x.compareAndSetField(1, 2) != false) return "FAIL Int: 2"
    if (::x.compareAndExchangeField(2, 1) != 2) return "FAIL Int: 3"
    if (::x.compareAndExchangeField(2, 1) != 1) return "FAIL Int: 4"
    if (::x.getAndSetField(3) != 1) return "FAIL Int: 5"
    if (::x.getAndSetField(1) != 3) return "FAIL Int: 6"
    if (::x.getAndAddFieldLocal(1) != 1) return "FAIL Int: 7"
    if (::x.getAndAddFieldLocal(1) != 2) return "FAIL Int: 8"
    if (x != 3) return "FAIL Int: 9"

    if (::y.compareAndSetField(1L, 2L) != true) return "FAIL Long: 1"
    if (::y.compareAndSetField(1L, 2L) != false) return "FAIL Long: 2"
    if (::y.compareAndExchangeField(2L, 1L) != 2L) return "FAIL Long: 3"
    if (::y.compareAndExchangeField(2L, 1L) != 1L) return "FAIL Long: 4"
    if (::y.getAndSetField(3L) != 1L) return "FAIL Long: 5"
    if (::y.getAndSetField(1L) != 3L) return "FAIL Long: 6"
    if (::y.getAndAddFieldLocal(1L) != 1L) return "FAIL Long: 7"
    if (::y.getAndAddFieldLocal(1L) != 2L) return "FAIL Long: 8"
    if (y != 3L) return "FAIL Long: 9"

    if (::z.compareAndSetField(a, b) != true) return "FAIL String: 1"
    if (::z.compareAndSetField(a, b) != false) return "FAIL String: 2"
    if (::z.compareAndExchangeField(b, a) != b) return "FAIL String: 3"
    if (::z.compareAndExchangeField(b, a) != a) return "FAIL String: 4"
    if (::z.getAndSetField(c) != a) return "FAIL String: 5"
    if (::z.getAndSetField(a) != c) return "FAIL String: 6"
    if (z != a) return "FAIL String: 7"

    if (::t.compareAndSetField(true, false) != true) return "FAIL Bool: 1"
    if (::t.compareAndSetField(true, false) != false) return "FAIL Bool: 2"
    if (::t.compareAndExchangeField(false, true) != false) return "FAIL Bool: 3"
    if (::t.compareAndExchangeField(false, true) != true) return "FAIL Bool: 4"
    if (::t.getAndSetField(false) != true) return "FAIL Bool: 5"
    if (::t.getAndSetField(true) != false) return "FAIL Bool: 6"
    if (t != true) return "FAIL Bool: 7"

    return "OK"
}
