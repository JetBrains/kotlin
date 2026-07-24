// TARGET_BACKEND: WASM
// LANGUAGE: +CompanionBlocks +CompanionExtensions

// MODULE: main
// FILE: externals.kt

class Test {
    companion {
        fun bar(value: String = "O"): String = value

        val foo = "K"
        var mutable = "INITIAL"
    }
}

@JsExport
fun companionBarDefault(): String = Test.bar()

@JsExport
fun companionBar(value: String): String = Test.bar(value)

@JsExport
fun companionFoo(): String = Test.foo

@JsExport
fun companionMutable(): String = Test.mutable

@JsExport
fun setCompanionMutable(value: String): String {
    Test.mutable = value
    return Test.mutable
}

fun box(): String = "OK"

// FILE: entry.mjs

import {
    companionBarDefault,
    companionBar,
    companionFoo,
    companionMutable,
    setCompanionMutable,
} from "./index.mjs"

if (companionBarDefault() !== "O") {
    throw "FAIL: bar default";
}
if (companionBar("CHANGED") !== "CHANGED") {
    throw "FAIL: bar argument";
}
if (companionFoo() !== "K") {
    throw "FAIL: foo";
}
if (companionMutable() !== "INITIAL") {
    throw "FAIL: mutable initial";
}
if (setCompanionMutable("CHANGED") !== "CHANGED") {
    throw "FAIL: mutable write";
}
if (companionMutable() !== "CHANGED") {
    throw "FAIL: mutable read after write";
}
