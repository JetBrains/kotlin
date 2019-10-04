// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

class A {
    class Nested(val result: String)
    inner class Inner(val result: String)
}

fun box(): String {
    return (A::Nested).call("O").result + (A::Inner).call((::A).call(), "K").result
}
