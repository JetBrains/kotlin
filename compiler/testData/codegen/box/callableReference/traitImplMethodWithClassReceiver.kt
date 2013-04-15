trait T {
    fun foo() = "OK"
}

class B : T {
    inner class C {
        fun bar() = this@B.(::foo)()
    }
}

fun box() = B().C().bar()
