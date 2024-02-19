// CHECK_TYPESCRIPT_DECLARATIONS
// TARGET_BACKEND: WASM
// MODULE: main
// FILE: nullableJsPrimitives.kt

@JsExport
fun produceBoolean(): JsBoolean? = true.toJsBoolean()

@JsExport
fun produceNumber(): JsNumber? = Int.MAX_VALUE.toJsNumber()

@JsExport
fun produceBigInt(): JsBigInt? = Long.MAX_VALUE.toJsBigInt()

@JsExport
fun produceString(): JsString? = "OK".toJsString()

@JsExport
fun produceAny(): JsAny? = 42.toJsNumber()

@JsExport
fun consumeBoolean(x: JsBoolean?): String? = x?.toBoolean()?.toString()

@JsExport
fun consumeNumber(x: JsNumber?): String? = x?.toInt()?.toString()

@JsExport
fun consumeBigInt(x: JsBigInt?): String? = x?.toLong()?.toString()

@JsExport
fun consumeString(x: JsString?): String? = x?.toString()

@JsExport
fun consumeAny(x: JsAny?): String? = x?.toString()

// FILE: entry.mjs

import {
    produceBoolean,
    produceNumber,
    produceBigInt,
    produceString,
    produceAny,
    consumeBoolean,
    consumeNumber,
    consumeBigInt,
    consumeAny,
} from "./index.mjs"

// PRODUCING
if (!produceBoolean()) throw new Error("Unexpected value was returned from the `produceBoolean` function")
if (produceNumber() != 2147483647) throw new Error("Unexpected value was returned from the `produceNumber` function")
if (produceBigInt() != 9223372036854775807n) throw new Error("Unexpected value was returned from the `produceBigInt` function")
if (produceString() != "OK") throw new Error("Unexpected value was returned from the `produceString` function")
if (produceAny() != 42) throw new Error("Unexpected value was returned from the `produceAny` function")

// CONSUMPTION
if (consumeBoolean(false) != "false") throw new Error("Unexpected value was returned from the `consumeBoolean` function")
if (consumeNumber(-2147483648) != "-2147483648") throw new Error("Unexpected value was returned from the `consumeNumber` function")
if (consumeBigInt(-9223372036854775808n) != "-9223372036854775808") throw new Error("Unexpected value was returned from the `consumeBigInt` function")
if (consumeAny(24) != 24) throw new Error("Unexpected value was returned from the `consumeAny` function")
