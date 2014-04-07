trait A {
    fun foo(): String = "A"
}

trait B {
    fun foo(): Any
}

class C : A, B

fun box(): String {
    val c = C()
    var result = ""
    result += c.foo()
    result += (c : B).foo()
    result += (c : A).foo()
    return if (result == "AAA") "OK" else "Fail: $result"
}
