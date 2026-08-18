// WITH_STDLIB
// RUN_THIRD_PARTY_OPTIMIZER

// KT-88434
//
// Two distinct external function types whose only difference is their (external)
// argument/return type collapse to the same JS-erased signature string
// `(Js)->Js` in JsInteropFunctionsLowering. The JS->Kotlin closure converter is
// cached per that signature string, so the converter created for the first
// type is reused for the second, carrying the first type's runtime checks
// (e.g. an instanceof/ref.test for `Array`). When the reused converter is
// invoked on a value of the second type the check fails and a
// ClassCastException is thrown.

@JsName("Array")
external class A : JsAny {
    val length: Int
}

external interface B : JsAny {
    val x: Int
}

fun array(): A = js("[]")
fun obj(): B = js("({ x: 1 })")

fun arrayClosure(): (A) -> A = js("x => x")
fun objectClosure(): (B) -> B = js("x => x")

fun box(): String {
    val a: A = arrayClosure()(array())
    if (a.length != 0) return "Fail: array closure returned wrong length"

    val b: B = objectClosure()(obj())
    if (b.x != 1) return "Fail: object closure returned wrong x"

    return "OK"
}
