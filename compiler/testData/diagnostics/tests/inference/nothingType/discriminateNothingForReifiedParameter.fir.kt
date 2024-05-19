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
    take(null)
}

fun test2() {
    takeReified(null)
}

fun test3() {
    <!REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>takeReifiedUnbound<!>(null)
}

fun test4(): Bound = takeReifiedUnbound(null)

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
        materializeReifiedUnbound()
    )

fun test8() {
    select(
        null,
        materializeReifiedUnbound()
    )
}
