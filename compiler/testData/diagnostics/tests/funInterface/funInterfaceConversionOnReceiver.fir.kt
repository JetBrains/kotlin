fun interface Bar {
    fun invoke(): String
}

operator fun Bar.plus(b: Bar): String = invoke() + b.invoke()

fun box(): String {
    return { "O" } <!INAPPLICABLE_CANDIDATE!>+<!> { "K" }
}
