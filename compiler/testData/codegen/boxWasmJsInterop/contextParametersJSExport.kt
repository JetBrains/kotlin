// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: WASM
// TARGET_BACKEND: WASM

// FILE: jsExport.kt
external interface Context {
    fun action(): String
}

context(c: Context)
fun test(): String = c.action()

@JsExport
fun usage1(c: Context): String = context(c) { test() }

@JsExport
fun usage2(c: Context): String = with(c) { test() }

context(c: Context)
private fun usage3Internal(): String = test()

@JsExport
fun usage3(c: Context): String = context(c) { usage3Internal() }

@JsExport
fun runAll(c: Context): String =
    if (usage1(c) == "OK" && usage2(c) == "OK" && usage3(c) == "OK") "OK"
    else "FAIL"

fun box(): String = "OK"

// FILE: entry.mjs
import { usage1, usage2, usage3, runAll } from "./index.mjs";

const ctx = { action: () => "OK" };

if (usage1(ctx) !== "OK") throw "Fail 1";
if (usage2(ctx) !== "OK") throw "Fail 2";
if (usage3(ctx) !== "OK") throw "Fail 3";
if (runAll(ctx) !== "OK") throw "Fail 4";