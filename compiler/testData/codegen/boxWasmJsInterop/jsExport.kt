// MODULE: main
// FILE: externals.kt

class C(val x: Int)

@JsExport
fun makeC(x: Int): C = C(x)

@JsExport
fun getX(c: C): Int = c.x

@JsExport
fun getString(s: String): String = "Test string $s";

@JsExport
fun isEven(x: Int): Boolean = x % 2 == 0

external interface EI

@JsExport
fun eiAsAny(ei: EI): Any = ei

@JsExport
fun anyAsEI(any: Any): EI = any as EI

fun box(): String = "OK"

// FILE: entry.mjs

import main from "./index.mjs"

const c = main.makeC(300);
if (main.getX(c) !== 300) {
    throw "Fail 1";
}

if (main.getString("2") !== "Test string 2") {
    throw "Fail 2";
}

if (main.isEven(31) !== false || main.isEven(10) !== true) {
    throw "Fail 3";
}

if (main.anyAsEI(main.eiAsAny({x:10})).x !== 10) {
    throw "Fail 4";
}