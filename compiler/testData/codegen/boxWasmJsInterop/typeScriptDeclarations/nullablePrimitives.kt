// CHECK_TYPESCRIPT_DECLARATIONS
// TARGET_BACKEND: WASM
// MODULE: main
// FILE: nullablePrimitives.kt

@JsExport
fun produceBoolean(): Boolean? = true

@JsExport
fun produceByte(): Byte? = Byte.MAX_VALUE

@JsExport
fun produceShort(): Short? = Short.MAX_VALUE

@JsExport
fun produceInt(): Int? = Int.MAX_VALUE

@JsExport
fun produceLong(): Long? = Long.MAX_VALUE

@JsExport
fun produceChar(): Char? = 'a'

@JsExport
fun produceString(): String? = "OK"

@JsExport
fun produceFunction(): (() -> Int)? = { 42 }

@JsExport
fun consumeBoolean(x: Boolean?): String? = x?.toString()

@JsExport
fun consumeByte(x: Byte?): String? = x?.toString()

@JsExport
fun consumeShort(x: Short?): String? = x?.toString()

@JsExport
fun consumeInt(x: Int?): String? = x?.toString()

@JsExport
fun consumeLong(x: Long?): String? = x?.toString()

@JsExport
fun consumeChar(x: Char?): String? = x?.toString()

@JsExport
fun consumeString(x: String?): String? = x

@JsExport
fun consumeFunction(fn: ((String) -> Int)?): Int? = fn?.invoke("42")

// FILE: entry.mjs

import {
    produceBoolean,
    produceByte,
    produceShort,
    produceInt,
    produceLong,
    produceChar,
    produceString,
    produceFunction,
    consumeBoolean,
    consumeByte,
    consumeShort,
    consumeInt,
    consumeLong,
    consumeChar,
    consumeString,
    consumeFunction,
} from "./index.mjs"

// PRODUCING
if (!produceBoolean()) throw new Error("Unexpected value was returned from the `produceBoolean` function")
if (produceByte() != 127) throw new Error("Unexpected value was returned from the `produceByte` function")
if (produceShort() != 32767) throw new Error("Unexpected value was returned from the `produceShort` function")
if (produceInt() != 2147483647) throw new Error("Unexpected value was returned from the `produceInt` function")
if (produceLong() != 9223372036854775807n) throw new Error("Unexpected value was returned from the `produceLong` function")
if (String.fromCharCode(produceChar()) != "a") throw new Error("Unexpected value was returned from the `produceChar` function")
if (produceString() != "OK") throw new Error("Unexpected value was returned from the `produceString` function")
if (produceFunction()() != 42) throw new Error("Unexpected value was returned from the `produceFunction` function")

// CONSUMPTION
if (consumeBoolean(false) != "false") throw new Error("Unexpected value was returned from the `consumeBoolean` function")
if (consumeByte(-128) != "-128") throw new Error("Unexpected value was returned from the `consumeByte` function")
if (consumeShort(-32768) != "-32768") throw new Error("Unexpected value was returned from the `consumeShort` function")
if (consumeInt(-2147483648) != "-2147483648") throw new Error("Unexpected value was returned from the `consumeInt` function")
if (consumeLong(-9223372036854775808n) != "-9223372036854775808") throw new Error("Unexpected value was returned from the `consumeLong` function")
if (consumeChar("b".charCodeAt()) != "b") throw new Error("Unexpected value was returned from the `consumeChar` function")
if (consumeString("🙂") != "🙂") throw new Error("Unexpected value was returned from the `consumeString` function")
if (consumeFunction(parseInt) != 42) throw new Error("Unexpected value was returned from the `consumeFunction` function")
