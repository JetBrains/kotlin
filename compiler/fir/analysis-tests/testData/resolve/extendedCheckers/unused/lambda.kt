fun f(t: (v: Int) -> Unit) {
    1.run(t)
}

fun main() {
    f { <!UNUSED_ANONYMOUS_PARAMETER!>i<!> ->

    }
}
