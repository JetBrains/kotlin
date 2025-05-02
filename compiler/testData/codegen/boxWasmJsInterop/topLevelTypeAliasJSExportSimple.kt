// TARGET_BACKEND: WASM
// !OPT_IN: kotlin.wasm.js.ExperimentalJsExport
// WITH_STDLIB

// FILE: jsExport.kt
typealias TA = UInt
typealias TB = TA
typealias TNullable = TA?
typealias ToStr = (TA) -> String

@JsExport
fun acceptTA(x: TA): String = x.toString()

@JsExport
fun acceptTB(x: TB): String = x.toString()

@JsExport
fun addAsString(x: TA, y: TB): String = (x + y).toString()

@JsExport
fun orZero(x: TNullable): String = (x ?: 0u).toString()

@JsExport
fun applyToStr(f: ToStr, x: TA): String = f(x)

fun box(): String = "OK"

// FILE: entry.mjs
import { acceptTA, acceptTB, addAsString, orZero, applyToStr } from "./index.mjs";

if (acceptTA(0) !== "0") throw "Fail 1";
if (acceptTB(1) !== "1") throw "Fail 2";
if (addAsString(1, 2) !== "3") throw "Fail 3";
if (orZero(null) !== "0") throw "Fail 4";
if (applyToStr((n) => "v" + n, 7) !== "v7") throw "Fail 5";;