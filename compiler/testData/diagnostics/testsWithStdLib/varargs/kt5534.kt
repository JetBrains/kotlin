fun test() {
    val p: Array<String> = arrayOf("a")
    foo(*p)
}

fun foo(vararg a: String?) = a