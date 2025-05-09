// LANGUAGE: +ContextParameters
// TARGET_BACKEND: WASM

// FILE: jsExport.kt
external interface ContextA { fun value(): String }
external interface ContextB { fun value(): String }

context(a: ContextA)
fun resolveValue(): String = a.value()

@JsExport
fun useAContextBlock(ctxA: ContextA): String = context(ctxA) { resolveValue() }

@JsExport
fun useAWith(ctxA: ContextA): String = with(ctxA) { resolveValue() }

context(a: ContextA)
private fun nestedInternal(): String = resolveValue()

@JsExport
fun useANested(ctxA: ContextA): String = context(ctxA) { nestedInternal() }

//EXPORT_DECLARATION_WITH_CONTEXT_PARAMETERS
//@JsExport
//context(a: ContextA)
//fun callValue(): String = resolveValue()
//
//@JsExport
//context(a: ContextA)
//fun callValueWithPrefix(prefix: String): String = prefix + resolveValue()
//
//@JsExport
//context(a: ContextA, b: ContextB)
//fun combineValues(): String = a.value() + b.value()

@JsExport
fun runAll(ctxA: ContextA): String =
    if (useAContextBlock(ctxA) == "OK" && useAWith(ctxA) == "OK" && useANested(ctxA) == "OK") "OK"
    else "FAIL"

fun box(): String = "OK"

// FILE: entry.mjs
import {
useAContextBlock,
useAWith,
useANested,
runAll
//callValue,
//callValueWithPrefix,
//combineValues
} from "./index.mjs";

const ctxA = { value: () => "OK" };

if (useAContextBlock(ctxA) !== "OK") throw "Fail 1";
if (useAWith(ctxA) !== "OK") throw "Fail 2";
if (useANested(ctxA) !== "OK") throw "Fail 3";
if (runAll(ctxA) !== "OK") throw "Fail 4";
//
//if (callValue(ctxA) !== "OK") throw "Fail 5";
//if (callValueWithPrefix(ctxA, "->") !== "->OK") throw "Fail 6";
//
//const a = { value: () => "OK" };
//const b = { value: () => "B" };
//if (combineValues(a, b) !== "OKB") throw "Fail 7";