open class Bar {
    fun foo() {}
}

fun test() {
    open class BaseLocal : Bar() {
        fun baz() {}
    }

    val base = BaseLocal()
    base.baz()
    base.foo()

    val anonymous = object : Bar() {
        fun baz() {}
    }
    anonymous.baz()
    anonymous.foo()

    class DerivedLocal : BaseLocal() {
        fun gau() {}
    }

    val derived = DerivedLocal()
    derived.gau()
    derived.baz()
    derived.foo()
}