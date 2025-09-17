// TARGET_BACKEND: WASM
// USE_SHARED_OBJECTS
// WASM_FAILS_IN: SM, JSC
// MODULE: main


// FILE: shared.kt

abstract class MyClassBase {
    abstract fun foo(): Int
}

class MyClass(val a: Int, val b: Int) : Function2<Int, Int, Int>, MyClassBase() {
    override fun invoke(p1: Int, p2: Int): Int = p1 - a + p2 * b

    override fun foo() = 5
}

var myObj = MyClass(1, 2)

fun box(): String {
    val myObjAsBase: MyClassBase = myObj
    val myObjAsFunc: Function2<Int, Int, Int> = myObj
    return if (myObj.a == 1 && myObjAsFunc.invoke(3, 4) == 10 && myObjAsBase.foo() == 5) "OK" else "Fail"
}

class C(val x: Int)

@JsExport
fun makeC(x: Int): JsReference<C> = C(x).toJsReference()

@JsExport
fun getX(c: JsReference<C>): Int = c.get().x

// FILE: entry.mjs

import {
    makeC,
    getX,
} from "./index.mjs"

const c = makeC(300);
if (getX(c) !== 300) {
    throw "Fail 1";
}