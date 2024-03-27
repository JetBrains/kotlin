// ISSUE: KT-63416

import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
suspend fun <Result> callOnceSuspending(block: suspend () -> Result): Result {
    <!LEAKED_IN_PLACE_LAMBDA!>contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }<!>

    try {
        return <!LEAKED_IN_PLACE_LAMBDA!>block<!>()
    } finally {
        println("some cleanup")
    }
}
