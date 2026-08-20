// TARGET_BACKEND: WASM
// RUN_THIRD_PARTY_OPTIMIZER

// MODULE: main
// FILE: externals.js

function foo(f) {
    return f(4, 5);
}

class A {
    constructor(value) {
        this.value = value;
    }
}

class B {
    constructor(value) {
        this.value = value;
    }
}

function foo2(f) {
    return f(new A(7));
}

function foo3(f) {
    return f(new B(8));
}

function foo4(f) {
    return f({ value: 9 });
}

// FILE: main.kt

external fun foo(f: (Int, Int) -> Int): Int

external class A {
    val value: Int
}

external class B {
    val value: Int
}

external fun foo2(f: (A) -> Int): Int
external fun foo3(f: (B) -> Int): Int

external fun <T : JsAny?> foo4(f: (T) -> Int): Int

fun box(): String {
    if (foo { x, y -> x + y } != 9) return "Fail: foo"
    if (foo2 { it.value + 1 } != 8) return "Fail: foo2"
    if (foo3 { it.value + 2 } != 10) return "Fail: foo3"
    if (foo4<JsAny> { 11 } != 11) return "Fail: foo4"

    return "OK"
}

// FILE: entry.mjs
import * as generated from "./index.mjs"

function assertNoCallFunctionExports(namespaceName, namespace) {
    const leaked = Object.keys(namespace).filter((name) => name.startsWith("__callFunction_"))
    if (leaked.length !== 0) {
        throw Error(`${namespaceName} leaks __callFunction_* exports: ${leaked.join(", ")}`)
    }
}

if (generated.box() !== "OK") {
    throw Error("Wrong box result")
}

assertNoCallFunctionExports("main named exports", generated)

if (Object.hasOwn(generated, "__ALL_EXPORTS")) {
    assertNoCallFunctionExports("main raw exports", generated.__ALL_EXPORTS)
}
