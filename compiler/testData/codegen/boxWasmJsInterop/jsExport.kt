// MODULE: main
// FILE: externals.kt

class C(val x: Int)

@JsExport
fun makeC(x: Int): C = C(x)

@JsExport
fun getX(c: C): Int = c.x

fun box(): String = "OK"

// FILE: jsExport__after.js

const c = main.makeC(300);
if (main.getX(c) !== 300) {
    throw "Fail 1";
}