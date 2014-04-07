var result = ""

trait A {
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
    (c : B).foo = "2"
    (c : A).foo = "3"
    return if (result == "123") "OK" else "Fail: $result"
}
