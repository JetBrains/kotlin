// !DIAGNOSTICS: -UNUSED_VARIABLE
annotation class Ann(vararg val i: Boolean)
fun foo() {
    val a1 = 1 > 2
    val a2 = 1 == 2
    val a3 = a1 == a2
    val a4 = a1 > a2

    @Ann(
            a1,
            a2,
            a3,
            a1 > a2,
            a1 == a2
    ) val b = 1
}