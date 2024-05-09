// WITH_EXTENDED_CHECKERS

fun foo(a: (Int) -> Unit) {}
fun foo(a: (Int, Int) -> Unit) {}
fun baz() {}
fun baz(p1: Int) {}

fun bar1() {
    foo { <!UNUSED_ANONYMOUS_PARAMETER!>p1<!>: Int -> baz() }
}

fun bar2() {
    foo { p1: Int -> baz(p1) }
}

fun bar3() {
    foo { p1: Int, <!UNUSED_ANONYMOUS_PARAMETER!>p2<!>: Int -> baz(p1) }
}

fun bar4() {
    foo { <!UNUSED_ANONYMOUS_PARAMETER!>p1<!>: Int, <!UNUSED_ANONYMOUS_PARAMETER!>p2<!>: Int -> baz() }
}

fun bar5() {
    foo { p1: Int, p2: Int ->
        baz(p1)
        baz(p1)
        baz(p2)
    }
}

fun bar6() {
    foo { a ->
        foo { <!UNUSED_ANONYMOUS_PARAMETER!>b<!> ->
            foo { c ->
                baz(a)
                baz(c)
            }
        }
    }
}

fun bar7() {
    foo { a ->
        val b = fun() {
            baz(a)
        }
        b()
    }
}
