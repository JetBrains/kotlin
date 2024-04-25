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
    <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.String)?")!>test1<!>

    val test2 = if (bool) {
        id { "2" }
    } else null
    <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.String)?")!>test2<!>

    val test3 = if (bool) {
        Inv { "3" }
    } else null
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<() -> kotlin.String>?")!>test3<!>

    val test4 = if (bool) {
        4 to { "4" }
    } else null
    <!DEBUG_INFO_EXPRESSION_TYPE("Pair<kotlin.Int, () -> kotlin.String>?")!>test4<!>

    val test5 = if (bool) {
        {{ "5" }}
    } else null
    <!DEBUG_INFO_EXPRESSION_TYPE("(() -> () -> kotlin.String)?")!>test5<!>

    val test6 = if (bool) {
        id { { "6" } }
    } else null
    <!DEBUG_INFO_EXPRESSION_TYPE("(() -> () -> kotlin.String)?")!>test6<!>

    val test7 = if (bool) {
        Inv { { "7" } }
    } else null
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<() -> () -> kotlin.String>?")!>test7<!>

    val test8 = if (bool) {
        8 to { { "8" } }
    } else null
    <!DEBUG_INFO_EXPRESSION_TYPE("Pair<kotlin.Int, () -> () -> kotlin.String>?")!>test8<!>

    val test9 = select({ "9" }, null)
    <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.String)?")!>test9<!>

    val test10 = select(id { "10" }, null)
    <!DEBUG_INFO_EXPRESSION_TYPE("(() -> kotlin.String)?")!>test10<!>

    val test11 = select(null, Inv { "11" })
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<() -> kotlin.String>?")!>test11<!>

    val test12 = select({ 12 to "" }, null)
    <!DEBUG_INFO_EXPRESSION_TYPE("(() -> Pair<kotlin.Int, kotlin.String>)?")!>test12<!>

    val test13: Pair<Int, () -> () -> String>? = if(bool) {
        13 to { { "13" } }
    } else null
    <!DEBUG_INFO_EXPRESSION_TYPE("Pair<kotlin.Int, () -> () -> kotlin.String>?")!>test13<!>
}