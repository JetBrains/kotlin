open class KFoo {
    fun foo(): String {
        if (this is KFooQux) return qux
        throw AssertionError()
    }
}

class KFooQux : KFoo()

val KFooQux.qux get() = "OK"

fun box() = KFooQux().foo()