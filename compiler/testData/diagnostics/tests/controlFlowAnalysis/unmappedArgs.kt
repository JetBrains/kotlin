fun foo(a: Int, b: Int) = a + b

fun bar(i: Int) {
    foo(1, 1, <!TOO_MANY_ARGUMENTS!>i<!>)
}
