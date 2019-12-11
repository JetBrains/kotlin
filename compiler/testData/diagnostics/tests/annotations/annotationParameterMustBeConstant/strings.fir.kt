// !DIAGNOSTICS: -UNUSED_VARIABLE
annotation class Ann(vararg val i: String)

const val topLevel = "topLevel"

fun foo() {
    val a1 = "a"
    val a2 = "b"
    val a3 = a1 + a2

    val a4 = 1
    val a5 = 1.0

    @Ann(
            a1,
            a2,
            a3,
            "$topLevel",
            "$a1",
            "$a1 $topLevel",
            "$a4",
            "$a5",
            a1 + a2,
            "a" + a2,
            "a" + topLevel,
            "a" + a4
    ) val b = 1
}