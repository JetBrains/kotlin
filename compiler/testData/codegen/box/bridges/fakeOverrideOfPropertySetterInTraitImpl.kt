var result = ""

interface A {
    var foo: String
        get() = result
        set(value) {
            result += value
        }
}

abstract class B {
    abstract var foo: Any
}

class C : A, B()

fun box(): String {
    val c = C()
    c.foo = "1"
    val b: B = c
    val a: A = c
    b.foo = "2"
    a.foo = "3"
    return if (result == "123") "OK" else "Fail: $result"
}
