// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER -UNREACHABLE_CODE

interface Bound

fun <T : Bound> take(t: T?): T = t ?: TODO()
inline fun <reified T : Bound> takeReified(t: T?): T = t ?: TODO()
inline fun <reified T> takeReifiedUnbound(t: T?): T = t ?: TODO()

fun <M : Bound> materialize(): M = TODO()
inline fun <reified M : Bound> materializeReified(): M = TODO()
inline fun <reified M> materializeReifiedUnbound(): M = TODO()

fun <T> select(a: T, b: T): T = TODO()

fun test1() {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>take(null)<!>
}

fun test2() {
    <!DEBUG_INFO_EXPRESSION_TYPE("Bound")!>takeReified(null)<!>
}

fun test3() {
    <!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>takeReifiedUnbound<!>(null)
}

fun test4(): Bound = <!DEBUG_INFO_EXPRESSION_TYPE("Bound")!>takeReifiedUnbound(null)<!>

fun test5(): Bound? = select(
    null,
    materialize()
)

fun test6() {
    select(
        null,
        <!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>materializeReified<!>()
    )
}

fun test7(): Bound? =
    select(
        null,
        <!DEBUG_INFO_EXPRESSION_TYPE("Bound?")!>materializeReifiedUnbound()<!>
    )

fun test8() {
    select(
        null,
        materializeReifiedUnbound()
    )
}
