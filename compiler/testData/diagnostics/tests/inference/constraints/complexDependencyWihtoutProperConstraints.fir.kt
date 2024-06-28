// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// Isuue: KT-37627

class Inv<T>(arg: T)
class Pair<A, B>
infix fun <M, N> M.to(other: N): Pair<M, N> = TODO()

fun <I> id(arg: I): I = arg
fun <S> select(vararg args: S): S = TODO()

fun test(bool: Boolean) {
    val test1 = if (bool) {
        { "1" }
    } else null
    test1

    val test2 = if (bool) {
        id { "2" }
    } else null
    test2

    val test3 = if (bool) {
        Inv { "3" }
    } else null
    test3

    val test4 = if (bool) {
        4 to { "4" }
    } else null
    test4

    val test5 = if (bool) {
        {{ "5" }}
    } else null
    test5

    val test6 = if (bool) {
        id { { "6" } }
    } else null
    test6

    val test7 = if (bool) {
        Inv { { "7" } }
    } else null
    test7

    val test8 = if (bool) {
        8 to { { "8" } }
    } else null
    test8

    val test9 = select({ "9" }, null)
    test9

    val test10 = select(id { "10" }, null)
    test10

    val test11 = select(null, Inv { "11" })
    test11

    val test12 = select({ 12 to "" }, null)
    test12

    val test13: Pair<Int, () -> () -> String>? = if(bool) {
        13 to { { "13" } }
    } else null
    test13
}