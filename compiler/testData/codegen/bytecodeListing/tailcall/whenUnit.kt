sealed class X {
    class A : X()
    class B : X()
}

suspend fun process(a: X.A) {}
suspend fun process(b: X.B) {}

suspend fun process(x: X) = when (x) {
    is X.A -> process(x)
    is X.B -> process(x)
}
