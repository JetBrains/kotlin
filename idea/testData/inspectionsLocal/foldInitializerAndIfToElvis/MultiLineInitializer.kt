fun foo(vararg args: String): String? = null

fun test(): Int {
    val foo = foo(
        "1111111111",
        "2222222222",
        "3333333333",
        "4444444444",
        "5555555555"
    )
    <caret>if (foo == null) return 0

    return 1
}