fun foo() {
    val a = 1
    val f = { x: Int ->
        val y = x + a
        use(a)
    }
}

fun use(vararg a: Any?) = a