// CHECK_TYPESCRIPT_DECLARATIONS
// TARGET_BACKEND: WASM
// MODULE: main
// FILE: unsigned.kt

@JsExport
fun produceUByte(): UByte = UByte.MAX_VALUE

@JsExport
fun produceUShort(): UShort = UShort.MAX_VALUE

@JsExport
fun produceUInt(): UInt = UInt.MAX_VALUE

@JsExport
fun produceULong(): ULong = ULong.MAX_VALUE

@JsExport
fun produceFunction(): () -> UInt = ::produceUInt

@JsExport
fun consumeUByte(x: UByte): String = x.toString()

@JsExport
fun consumeUShort(x: UShort): String = x.toString()

@JsExport
fun consumeUInt(x: UInt): String = x.toString()

@JsExport
fun consumeULong(x: ULong): String = x.toString()

@JsExport
fun consumeFunction(fn: (String) -> UInt): UInt = fn("42")

// FILE: entry.mjs

import main from "./index.mjs"

// PRODUCING
if (main.produceUByte() != 255) throw new Error("Unexpected value was returned from the `produceUByte` function")
if (main.produceUShort() != 65535) throw new Error("Unexpected value was returned from the `produceUShort` function")
if (main.produceUInt() != 4294967295) throw new Error("Unexpected value was returned from the `produceUInt` function")
if (main.produceULong() != 18446744073709551615n) throw new Error("Unexpected value was returned from the `produceULong` function")
if (main.produceFunction()() != 4294967295) throw new Error("Unexpected value was returned from the `produceFunction` function")

// CONSUMPTION
if (main.consumeUByte(-128) != "128") throw new Error("Unexpected value was returned from the `consumeUByte` function")
if (main.consumeUShort(-32768) != "32768") throw new Error("Unexpected value was returned from the `consumeUShort` function")
if (main.consumeUInt(-2147483648) != "2147483648") throw new Error("Unexpected value was returned from the `consumeUInt` function")
if (main.consumeULong(-9223372036854775808n) != "9223372036854775808") throw new Error("Unexpected value was returned from the `consumeULong` function")
if (main.consumeFunction(parseInt) != 42) throw new Error("Unexpected value was returned from the `consumeFunction` function")
