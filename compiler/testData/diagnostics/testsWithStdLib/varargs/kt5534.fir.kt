fun test() {
    val p: Array<String> = arrayOf("a")
    <!INAPPLICABLE_CANDIDATE!>foo<!>(*p)
}

fun foo(vararg a: String?) = a