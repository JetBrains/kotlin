// KT-44050
enum class Enum {
    Entry1() {
        fun bogus() = 42
    },
    Entry2() {
        fun bogus() = 42
    }
}

class Outer {
    fun barCaller(): Enum = obj1.bar()
    fun bazCaller(): Enum = obj2.baz()

    private abstract inner class Inner<T>(val default: T) {
        abstract fun foo()
    }

    private val obj1 = object : Inner<Enum>(Enum.Entry1) {
        override fun foo() {
            TODO("not related")
        }

        fun bar(): Enum {
            return default
        }
    }

    private val obj2 = object : Inner<Enum>(Enum.Entry2) {
        override fun foo() {
            TODO("not related")
        }

        fun baz(): Enum {
            return default
        }
    }
}

fun box(): String {
    val o = Outer()
    if (o.barCaller() != Enum.Entry1) return "Fail1"
    if (o.bazCaller() != Enum.Entry2) return "Fail2"
    return "OK"
}
