// TARGET_BACKEND: JS_IR
// WITH_STDLIB

external interface A

external interface I {
    var foo: A
}

fun I.test1(d: dynamic) {
    if (d != null) {
        foo = d
    }
}

