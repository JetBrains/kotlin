interface T {
    fun foo() = "OK"
}

class B : T {
    inner class C {
        fun bar() = (T::foo).let { it(this@B) }
    }
}

fun box() = B().C().bar()
