fun foo(x: Int) {
    r {
        when (x) {
            2 -> <!UNUSED_EXPRESSION!>0<!>
        }
    }
}

fun r(f: () -> Unit) {
    f()
}