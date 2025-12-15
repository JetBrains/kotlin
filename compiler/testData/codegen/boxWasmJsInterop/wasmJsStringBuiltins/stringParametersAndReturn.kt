// TARGET_BACKEND: WASM
// WITH_STDLIB

// FILE: test.kt

@JsExport
fun decorate(prefix: String, value: String, suffix: String): String =
    prefix + value + suffix

@JsExport
fun echo(value: String): String = value

@JsExport
fun ktLength(value: String): Int = value.length

fun box(): String = "OK"

// FILE: entry.mjs
import { decorate, echo, ktLength } from "./index.mjs";

const cases = [
"",
"ASCII",
"Привет",
"🚀",
"e\u0301",
"a\u0000b",
"שלום",
];

for (let i = 0; i < cases.length; i++) {
    const s = cases[i];

    const r1 = echo(s);
    if (typeof r1 !== "string") throw `Fail1`;
    if (r1 !== s) throw `Fail2`;

    const r2 = decorate("<<", s, ">>");
    if (typeof r2 !== "string") throw `Fail3`;
    if (r2 !== `<<${s}>>`) throw `Fail4`;

    const l = ktLength(s);
    if (l !== s.length) throw `Fail5`;
}
