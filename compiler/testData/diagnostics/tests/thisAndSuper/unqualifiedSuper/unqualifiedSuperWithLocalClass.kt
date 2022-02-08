// FIR_IDENTICAL
interface Interface {
    fun foo(x: Int): Int
}

fun withLocalClasses(param: Int): Interface {
    open class LocalBase {
        open val param: Int
            get() = 100
    }

    <!LOCAL_INTERFACE_NOT_ALLOWED!>interface LocalInterface<!> : Interface {
        override fun foo(x: Int): Int =
                x + param
    }

    return object : LocalBase(), LocalInterface {
        override fun foo(x: Int): Int =
                x + super.param
    }

}