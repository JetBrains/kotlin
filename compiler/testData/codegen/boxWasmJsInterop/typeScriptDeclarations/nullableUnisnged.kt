// CHECK_TYPESCRIPT_DECLARATIONS
// TARGET_BACKEND: WASM
// MODULE: main
// FILE: nullableUnsigned.kt

@JsExport
fun produceUByte(): UByte? = UByte.MAX_VALUE

@JsExport
fun produceUShort(): UShort? = UShort.MAX_VALUE

@JsExport
fun produceUInt(): UInt? = UInt.MAX_VALUE

@JsExport
fun produceULong(): ULong? = ULong.MAX_VALUE

@JsExport
fun produceFunction(): () -> UInt? = ::produceUInt

@JsExport
fun consumeUByte(x: UByte?): String? = x?.toString()

@JsExport
fun consumeUShort(x: UShort?): String? = x?.toString()

@JsExport
fun consumeUInt(x: UInt?): String? = x?.toString()

@JsExport
fun consumeULong(x: ULong?): String? = x?.toString()

@JsExport
fun consumeFunction(fn: (String) -> UInt?): UInt? = fn("42")

// FILE: entry.mjs

import {
    produceUByte,
    produceUShort,
    produceUInt,
    produceULong,
    produceFunction,
    consumeUByte,
    consumeUShort,
    consumeUInt,
    consumeULong,
    consumeFunction,
} from "./index.mjs"

// PRODUCING
if (produceUByte() != 255) throw new Error("Unexpected value was returned from the `produceUByte` function")
if (produceUShort() != 65535) throw new Error("Unexpected value was returned from the `produceUShort` function")
if (produceUInt() != 4294967295) throw new Error("Unexpected value was returned from the `produceUInt` function")
if (produceULong() != 18446744073709551615n) throw new Error("Unexpected value was returned from the `produceULong` function")
if (produceFunction()() != 4294967295) throw new Error("Unexpected value was returned from the `produceFunction` function")

// CONSUMPTION
if (consumeUByte(-128) != "128") throw new Error("Unexpected value was returned from the `consumeUByte` function")
if (consumeUShort(-32768) != "32768") throw new Error("Unexpected value was returned from the `consumeUShort` function")
if (consumeUInt(-2147483648) != "2147483648") throw new Error("Unexpected value was returned from the `consumeUInt` function")
if (consumeULong(-9223372036854775808n) != "9223372036854775808") throw new Error("Unexpected value was returned from the `consumeULong` function")
if (consumeFunction(parseInt) != 42) throw new Error("Unexpected value was returned from the `consumeFunction` function")
