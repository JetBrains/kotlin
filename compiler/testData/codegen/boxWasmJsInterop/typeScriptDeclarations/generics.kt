// CHECK_TYPESCRIPT_DECLARATIONS
// TARGET_BACKEND: WASM
// MODULE: main

// FILE: generics.kt
@JsExport
fun <T: JsAny?> simple(x: T): T = x

@JsExport
fun <A: JsAny?, B: JsAny?> second(a: A, b: B): B = b

@JsExport
fun <T: JsNumber> simpleWithConstraint(x: T): T = x

external interface Foo<T: JsAny> : JsAny {
    val foo: T
}

external interface Bar : JsAny {
    val bar: JsString
}

external object Baz : JsAny {
    val baz: JsBoolean
}

fun getBaz(x: Foo<JsBigInt>): JsAny = js("({ baz: x.foo > 0n })")

@JsExport
fun <A, B> complexConstraint(x: A): B where A: Foo<JsBigInt>, A: Bar, B: Baz = getBaz(x).unsafeCast<B>()

// FILE: entry.mjs

import { simple, second, simpleWithConstraint, complexConstraint } from "./index.mjs";

if (simple("OK") != "OK") throw new Error("Unexpected result from `simple` function")
if (second(1, "OK") != "OK") throw new Error("Unexpected result from `second` function")
if (simpleWithConstraint(42) != 42) throw new Error("Unexpected result from `simpleConstraint` function")
if (JSON.stringify(complexConstraint({ foo: 1n, bar: "bar" })) != "{\"baz\":true}") throw new Error("Unexpected result from `complexConstraint` function")
