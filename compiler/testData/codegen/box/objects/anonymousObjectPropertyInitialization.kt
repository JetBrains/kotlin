interface T {
    fun foo(): String
}

val o = object : T {
    val a = "OK"
    val f = {
        a
    }()

    override fun foo() = f
}

fun box() = o.foo()
