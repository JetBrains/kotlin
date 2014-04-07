var result = ""

trait Base
open class Child : Base

trait A<T : Base> {
    fun <E : T> foo(a : E) {
        result += "A"
    }
}

class B : A<Child> {
    override fun <E : Child> foo(a : E) {
        result += "B"
    }
}

fun box(): String {
    val b = B()
    b.foo(Child())
    (b : A<Child>).foo(Child())
    return if (result == "BB") "OK" else "Fail: $result"
}
