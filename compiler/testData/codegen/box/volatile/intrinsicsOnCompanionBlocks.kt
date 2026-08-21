// LANGUAGE: +CompanionBlocks
// TARGET_BACKEND: NATIVE
// DISABLE_IR_VISIBILITY_CHECKS: ANY

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import kotlin.native.concurrent.*
import kotlin.concurrent.*
import kotlin.native.internal.*
import kotlin.reflect.KMutableProperty0

val a = "1"
val b = "2"
val c = "3"

class C {
    companion {
        @Volatile var byte: Byte = 1
        @Volatile var short: Short = 1
        @Volatile var x: Int = 1
        @Volatile var y: Long = 1L
        @Volatile var z: String = a
        @Volatile var t: Boolean = true
    }
}

fun box() : String {
    if (C::byte.compareAndSetField(1.toByte(), 2.toByte()) != true) return "FAIL Byte: 1"
    if (C::byte.compareAndSetField(1.toByte(), 2.toByte()) != false) return "FAIL Byte: 2"
    if (C::byte.compareAndExchangeField(2.toByte(), 1.toByte()) != 2.toByte()) return "FAIL Byte: 3"
    if (C::byte.compareAndExchangeField(2.toByte(), 1.toByte()) != 1.toByte()) return "FAIL Byte: 4"
    if (C::byte.getAndSetField(3.toByte()) != 1.toByte()) return "FAIL Byte: 5"
    if (C::byte.getAndSetField(1.toByte()) != 3.toByte()) return "FAIL Byte: 6"
    if (C::byte.getAndAddField(1.toByte()) != 1.toByte()) return "FAIL Byte: 7"
    if (C::byte.getAndAddField(1.toByte()) != 2.toByte()) return "FAIL Byte: 8"
    if (C.byte != 3.toByte()) return "FAIL Byte: 9"

    if (C::short.compareAndSetField(1.toShort(), 2.toShort()) != true) return "FAIL Short: 1"
    if (C::short.compareAndSetField(1.toShort(), 2.toShort()) != false) return "FAIL Short: 2"
    if (C::short.compareAndExchangeField(2.toShort(), 1.toShort()) != 2.toShort()) return "FAIL Short: 3"
    if (C::short.compareAndExchangeField(2.toShort(), 1.toShort()) != 1.toShort()) return "FAIL Short: 4"
    if (C::short.getAndSetField(3.toShort()) != 1.toShort()) return "FAIL Short: 5"
    if (C::short.getAndSetField(1.toShort()) != 3.toShort()) return "FAIL Short: 6"
    if (C::short.getAndAddField(1.toShort()) != 1.toShort()) return "FAIL Short: 7"
    if (C::short.getAndAddField(1.toShort()) != 2.toShort()) return "FAIL Short: 8"
    if (C.short != 3.toShort()) return "FAIL Short: 9"

    if (C::x.compareAndSetField(1, 2) != true) return "FAIL Int: 1"
    if (C::x.compareAndSetField(1, 2) != false) return "FAIL Int: 2"
    if (C::x.compareAndExchangeField(2, 1) != 2) return "FAIL Int: 3"
    if (C::x.compareAndExchangeField(2, 1) != 1) return "FAIL Int: 4"
    if (C::x.getAndSetField(3) != 1) return "FAIL Int: 5"
    if (C::x.getAndSetField(1) != 3) return "FAIL Int: 6"
    if (C::x.getAndAddField(1) != 1) return "FAIL Int: 7"
    if (C::x.getAndAddField(1) != 2) return "FAIL Int: 8"
    if (C.x != 3) return "FAIL Int: 9"

    if (C::y.compareAndSetField(1L, 2L) != true) return "FAIL Long: 1"
    if (C::y.compareAndSetField(1L, 2L) != false) return "FAIL Long: 2"
    if (C::y.compareAndExchangeField(2L, 1L) != 2L) return "FAIL Long: 3"
    if (C::y.compareAndExchangeField(2L, 1L) != 1L) return "FAIL Long: 4"
    if (C::y.getAndSetField(3L) != 1L) return "FAIL Long: 5"
    if (C::y.getAndSetField(1L) != 3L) return "FAIL Long: 6"
    if (C::y.getAndAddField(1L) != 1L) return "FAIL Long: 7"
    if (C::y.getAndAddField(1L) != 2L) return "FAIL Long: 8"
    if (C.y != 3L) return "FAIL Long: 9"

    if (C::z.compareAndSetField(a, b) != true) return "FAIL String: 1"
    if (C::z.compareAndSetField(a, b) != false) return "FAIL String: 2"
    if (C::z.compareAndExchangeField(b, a) != b) return "FAIL String: 3"
    if (C::z.compareAndExchangeField(b, a) != a) return "FAIL String: 4"
    if (C::z.getAndSetField(c) != a) return "FAIL String: 5"
    if (C::z.getAndSetField(a) != c) return "FAIL String: 6"
    if (C.z != a) return "FAIL String: 7"

    if (C::t.compareAndSetField(true, false) != true) return "FAIL Bool: 1"
    if (C::t.compareAndSetField(true, false) != false) return "FAIL Bool: 2"
    if (C::t.compareAndExchangeField(false, true) != false) return "FAIL Bool: 3"
    if (C::t.compareAndExchangeField(false, true) != true) return "FAIL Bool: 4"
    if (C::t.getAndSetField(false) != true) return "FAIL Bool: 5"
    if (C::t.getAndSetField(true) != false) return "FAIL Bool: 6"
    if (C.t != true) return "FAIL Bool: 7"

    return "OK"
}
