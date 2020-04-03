
fun foo1() = <!EXPRESSION_REQUIRED!>while (b()) {}<!>

fun foo2() = <!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>for (i in 10) {}<!>

fun foo3() = when (b()) {
    true -> 1
    else -> 0
}

fun b(): Boolean = true