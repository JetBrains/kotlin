fun takeAnyFun(function: Function<*>) {}

fun test(block: () -> Unit) {
    <!INAPPLICABLE_CANDIDATE!>takeAnyFun<!>(block)
}
