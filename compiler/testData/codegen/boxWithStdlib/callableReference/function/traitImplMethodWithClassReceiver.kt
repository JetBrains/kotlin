interface T {
    fun foo() = "OK"
}

class B : T {
    inner class C {
        fun bar() = (::foo)(this@B)
    }
}

fun box() = B().C().bar()
