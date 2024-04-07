// ISSUE: KT-63416

import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
suspend fun <Result> callOnceSuspending(block: suspend () -> Result): Result {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    try {
        return block()
    } finally {
        println("some cleanup")
    }
}
