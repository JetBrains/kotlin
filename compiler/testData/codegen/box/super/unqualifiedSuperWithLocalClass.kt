interface Interface {
    fun foo(x: Int): Int
}

fun withLocalClasses(param: Int): Interface {
    open class LocalBase {
        open val param: Int
            get() = 100
    }

    interface LocalInterface : Interface {
        override fun foo(x: Int): Int =
                x + param
    }

    return object : LocalBase(), LocalInterface {
        override fun foo(x: Int): Int =
                x + super.param
    }

}

fun box(): String {
    val t = withLocalClasses(1)

    val test1 = t.foo(10)
    if (test1 != 110) return "Fail: t.foo(10)==$test1"

    return "OK"
}