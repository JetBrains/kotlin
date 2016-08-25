class C {
    fun foo(): C = this
    fun bar(): C? = this
}

fun test(nc: C?) =
        nc?.foo()?.bar()?.foo()?.foo()