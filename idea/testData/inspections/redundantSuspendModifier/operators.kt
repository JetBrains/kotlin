// WITH_RUNTIME

class A(val x: Int) {
    // Redundant
    suspend operator fun plus(a: A): A {
        return A(x + a.x)
    }
}

// Not redundant
suspend fun foo(a1: A, a2: A): A {
    return a1 + a2
}
// Not redundant
suspend fun bar(a1: A, a2: A): A {
    var result = a1
    result += a2
    return result
}

class B(var x: Int) {
    // Redundant
    suspend operator fun minusAssign(b: B) {
        x -= b.x
    }
}

// Not redundant
suspend fun foo(b1: B, b2: B): B {
    val result = b1
    result -= b2
    return result
}

class C(val x: Int, val y: Int) {
    // Redundant
    suspend operator fun invoke() = x + y
}

// Not redundant
suspend fun foo(c1: C, c2: C): Int {
    return c1() + c2()
}