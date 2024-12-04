// TARGET_BACKEND: JVM
// WITH_REFLECT

class A {
    class Nested(val result: String)
    inner class Inner(val result: String)
}

fun box(): String {
    return (A::Nested).call("O").result + (A::Inner).call((::A).call(), "K").result
}
