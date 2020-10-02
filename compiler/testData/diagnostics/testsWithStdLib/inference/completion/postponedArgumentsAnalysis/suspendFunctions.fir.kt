// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER

fun <T> select(vararg x: T) = x[0]

fun <K> id(x: K): K = x

fun <T: suspend (Int) -> Unit, K: T> takeSuspend(x: T, y: K) = x
fun <T: (Int) -> Unit, K: T> takeSimpleFunction(x: T, y: K) = x

fun main() {
    select(suspend {}, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.coroutines.SuspendFunction0<kotlin.Unit>")!>{}<!>)
    select(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.coroutines.SuspendFunction0<kotlin.Unit>")!>{}<!>, suspend {})
    select(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.coroutines.SuspendFunction0<kotlin.Unit>")!>id {}<!>, suspend {})
    select(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.coroutines.SuspendFunction0<kotlin.Unit>")!>id {}<!>, id(suspend {}))
    select(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.coroutines.SuspendFunction0<kotlin.Unit>")!>id {}<!>, id<suspend () -> Unit> {})

    takeSuspend(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.coroutines.SuspendFunction1<kotlin.Int, kotlin.Int>")!>id { it }<!>, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.coroutines.SuspendFunction1<kotlin.Int, kotlin.Int>")!>{ x -> x }<!>)

    val x1: suspend (Int) -> Unit = takeSuspend(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.coroutines.SuspendFunction1<kotlin.Int, kotlin.Int>")!>id { it }<!>, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.coroutines.SuspendFunction1<kotlin.Int, kotlin.Int>")!>{ x -> x }<!>)

    // Here, the error should be
    val x2: (Int) -> Unit = takeSuspend(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.coroutines.SuspendFunction1<kotlin.Int, kotlin.Int>")!>id { it }<!>, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.coroutines.SuspendFunction1<kotlin.Int, kotlin.Int>")!>{ x -> x }<!>)
    val x3: suspend (Int) -> Unit = takeSimpleFunction(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.coroutines.SuspendFunction1<kotlin.Int, kotlin.Int>")!>id { it }<!>, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.coroutines.SuspendFunction1<kotlin.Int, kotlin.Int>")!>{ x -> x }<!>)
    val x4: (Int) -> Unit = <!INAPPLICABLE_CANDIDATE!>takeSimpleFunction<!>(id<suspend (Int) -> Unit> {}, <!DEBUG_INFO_EXPRESSION_TYPE("Type is unknown")!>{}<!>)
}
