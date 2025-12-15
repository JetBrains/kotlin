// TARGET_BACKEND: WASM
// WITH_STDLIB

// FILE: test.kt

@JsExport
fun echoK(value: String): String = value

@JsExport
fun makeK(): String = "A🚀\u0000e\u0301Z"

fun box(): String = "OK"

// FILE: entry.mjs
import { echoK, makeK } from "./index.mjs";

const cases = ["", "ASCII", "🚀\u0000e\u0301", "שלום"];

for (let i = 0; i < cases.length; i++) {
    const s = cases[i];
    const r = echoK(s);
    if (typeof r !== "string") throw `Fail1`;
    if (r instanceof String) throw `Fail2`;
    if (r !== s) throw `Fail3`;
}

const m = makeK();
if (typeof m !== "string") throw "Fail4";
if (m instanceof String) throw "Fail5";
if (m !== "A🚀\u0000e\u0301Z") throw `Fail6'`;
