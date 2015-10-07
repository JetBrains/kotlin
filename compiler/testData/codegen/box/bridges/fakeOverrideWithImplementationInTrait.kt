interface A {
    fun foo(): String = "A"
}

interface B {
    fun foo(): Any
}

class C : A, B

fun box(): String {
    val c = C()
    var result = ""
    val b: B = c
    val a: A = c
    result += c.foo()
    result += b.foo()
    result += a.foo()
    return if (result == "AAA") "OK" else "Fail: $result"
}
