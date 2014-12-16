fun test() {
    val p: Array<String> = array("a")
    foo(*p)
}

fun foo(vararg a: String?) = a