fun test1(f: (Int) -> Int) = f(1)

fun test2(f: Int.() -> Int) = 2.f()

class A(val foo: Int.() -> Int)

fun box(): String {
    val a: (Int) -> Int = { it }
    val b: Int.() -> Int = { this }

    if (test1(a) != 1) return "fail 1a"
    if (test1(b) != 1) return "fail 1b"
    if (test2(a) != 2) return "fail 2a"
    if (test2(b) != 2) return "fail 2b"

    val x = A({ this })

    if (x.foo(3) != 3) return "fail 3"
    if (with(x) { foo(4) } != 4) return "fail 4"
    if (with(x) { 5.foo() } != 5) return "fail 5"

    return "OK"
}