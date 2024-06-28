// TARGET_BACKEND: WASM
// FILE: externals.js
function provideUByte() { return -1 }
function provideNullableUByte(nullable) { return nullable ? null : - 1 }

function consumeUByte(x) { return x.toString() }
function consumeNullableUByte(x) { return x == null ? null : x.toString() }

function provideUShort() { return -1 }
function provideNullableUShort(nullable) { return nullable ? null : - 1 }

function consumeUShort(x) { return x.toString() }
function consumeNullableUShort(x) { return x == null ? null : x.toString() }

function provideUInt() { return -1 }
function provideNullableUInt(nullable) { return nullable ? null : - 1 }

function consumeUInt(x) { return x.toString() }
function consumeNullableUInt(x) { return x == null ? null : x.toString() }

function provideULong() { return -1n }
function provideNullableULong(nullable) { return nullable ? null : - 1n }

function consumeULong(x) { return x.toString() }
function consumeNullableULong(x) { return x == null ? null : x.toString() }

function consumeUByteVararg(x) { return x.toString() }
function consumeNullableUByteVararg(x) { return x == null ? null : x.toString() }

function consumeUShortVararg(x) { return x.toString() }
function consumeNullableUShortVararg(x) { return x == null ? null : x.toString() }

function consumeUIntVararg(x) { return x.toString() }
function consumeNullableUIntVararg(x) { return x == null ? null : x.toString() }

function consumeULongVararg(x) { return x.toString() }
function consumeNullableULongVararg(x) { return x == null ? null : x.toString() }

// FILE: externals.kt
external fun provideUByte(): UByte

external fun provideNullableUByte(nullable: Boolean): UByte?

external fun consumeUByte(x: UByte): String

external fun consumeNullableUByte(x: UByte?): String?

external fun provideUShort(): UShort

external fun provideNullableUShort(nullable: Boolean): UShort?

external fun consumeUShort(x: UShort): String

external fun consumeNullableUShort(x: UShort?): String?

external fun provideUInt(): UInt

external fun provideNullableUInt(nullable: Boolean): UInt?

external fun consumeUInt(x: UInt): String

external fun consumeNullableUInt(x: UInt?): String?

external fun provideULong(): ULong

external fun provideNullableULong(nullable: Boolean): ULong?

external fun consumeULong(x: ULong): String

external fun consumeNullableULong(x: ULong?): String?

external fun consumeUByteVararg(vararg shorts: UByte): String

external fun consumeNullableUByteVararg(vararg shorts: UByte?): String?

external fun consumeUShortVararg(vararg shorts: UShort): String

external fun consumeNullableUShortVararg(vararg shorts: UShort?): String?

external fun consumeUIntVararg(vararg ints: UInt): String

external fun consumeNullableUIntVararg(vararg ints: UInt?): String?

external fun consumeULongVararg(vararg ints: ULong): String

external fun consumeNullableULongVararg(vararg ints: ULong?): String?

fun box(): String {
    if (provideUByte() != UByte.MAX_VALUE) return "Fail 1"
    if (provideNullableUByte(false) != UByte.MAX_VALUE) return "Fail 2"
    if (provideNullableUByte(true) != null) return "Fail 3"

    if (provideUShort() != UShort.MAX_VALUE) return "Fail 4"
    if (provideNullableUShort(false) != UShort.MAX_VALUE) return "Fail 5"
    if (provideNullableUShort(true) != null) return "Fail 6"

    if (provideUInt() != UInt.MAX_VALUE) return "Fail 7"
    if (provideNullableUInt(false) != UInt.MAX_VALUE) return "Fail 8"
    if (provideNullableUInt(true) != null) return "Fail 9"

    if (provideULong() != ULong.MAX_VALUE) return "Fail 10"
    if (provideNullableULong(false) != ULong.MAX_VALUE) return "Fail 11"
    if (provideNullableULong(true) != null) return "Fail 12"

    if (consumeUByte(UByte.MAX_VALUE) != "255") return "Fail 13"
    if (consumeNullableUByte(UByte.MAX_VALUE) != "255") return "Fail 14"
    if (consumeNullableUByte(null) != null) return "Fail 15"

    if (consumeUShort(UShort.MAX_VALUE) != "65535") return "Fail 16"
    if (consumeNullableUShort(UShort.MAX_VALUE) != "65535") return "Fail 17"
    if (consumeNullableUShort(null) != null) return "Fail 18"

    if (consumeUInt(UInt.MAX_VALUE) != "4294967295") return "Fail 19"
    if (consumeNullableUInt(UInt.MAX_VALUE) != "4294967295") return "Fail 20"
    if (consumeNullableUInt(null) != null) return "Fail 21"

    if (consumeULong(ULong.MAX_VALUE) != "18446744073709551615") return "Fail 22"
    if (consumeNullableULong(ULong.MAX_VALUE) != "18446744073709551615") return "Fail 23"
    if (consumeNullableULong(null) != null) return "Fail 24"

    if (provideUShort() != UShort.MAX_VALUE) return "Fail 25"
    if (provideNullableUShort(false) != UShort.MAX_VALUE) return "Fail 26"
    if (provideNullableUShort(true) != null) return "Fail 27"

    if (provideUInt() != UInt.MAX_VALUE) return "Fail 28"
    if (provideNullableUInt(false) != UInt.MAX_VALUE) return "Fail 29"
    if (provideNullableUInt(true) != null) return "Fail 30"

    if (provideULong() != ULong.MAX_VALUE) return "Fail 31"
    if (provideNullableULong(false) != ULong.MAX_VALUE) return "Fail 32"
    if (provideNullableULong(true) != null) return "Fail 33"

    if (consumeUByteVararg(UByte.MAX_VALUE) != "255") return "Fail 34"
    if (consumeNullableUByteVararg(UByte.MAX_VALUE) != "255") return "Fail 35"
    if (consumeNullableUByteVararg(null) != null) return "Fail 36"

    if (consumeUShortVararg(UShort.MAX_VALUE) != "65535") return "Fail 37"
    if (consumeNullableUShortVararg(UShort.MAX_VALUE) != "65535") return "Fail 38"
    if (consumeNullableUShortVararg(null) != null) return "Fail 39"

    if (consumeUIntVararg(UInt.MAX_VALUE) != "4294967295") return "Fail 40"
    if (consumeNullableUIntVararg(UInt.MAX_VALUE) != "4294967295") return "Fail 41"
    if (consumeNullableUIntVararg(null) != null) return "Fail 42"

    if (consumeULongVararg(ULong.MAX_VALUE) != "18446744073709551615") return "Fail 43"
    if (consumeNullableULongVararg(ULong.MAX_VALUE) != "18446744073709551615") return "Fail 44"
    if (consumeNullableULongVararg(null) != null) return "Fail 45"

    return "OK"
}
