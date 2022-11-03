// FIR_DISABLE_LAZY_RESOLVE_CHECKS
interface Base {
    val x: Int
}

open class Impl(override val x: Int) : Base {
    init {
        if (this.<!DEBUG_INFO_LEAKING_THIS!>x<!> != 0) foo()
    }
}

fun foo() {}
