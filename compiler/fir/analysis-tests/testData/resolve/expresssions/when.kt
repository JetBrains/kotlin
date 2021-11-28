fun foo() = if (true) 1 else 0

fun bar(arg: Any?) = when (arg) {
    is Int -> arg <!USELESS_CAST!>as Int<!>
    else -> 42
}
