val foo: dynamic = 1

fun foo(x: dynamic): dynamic {
    class C {
        val foo: dynamic = 1
    }
    return x + C().foo
}
