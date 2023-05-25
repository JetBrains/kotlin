val (a, b) = A()
val (c, d) = B()

val rv = (a + b) * (c + d)

class A {
    operator fun component1() = 1
    operator fun component2() = 5
}

class B {
    operator fun component1() = 3
    operator fun component2() = 4
}

// expected: rv: 42