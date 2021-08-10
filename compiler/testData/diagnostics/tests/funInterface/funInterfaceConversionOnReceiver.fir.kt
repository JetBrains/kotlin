fun interface Bar {
    fun invoke(): String
}

operator fun Bar.plus(b: Bar): String = invoke() + b.invoke()

fun box(): String {
    return { "O" } <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>+<!> { "K" }
}
