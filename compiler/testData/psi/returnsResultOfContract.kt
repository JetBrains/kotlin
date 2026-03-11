// WITH_STDLIB
// COMPILER_ARGUMENTS: -Xreturn-value-checker=full
import kotlin.contracts.*

class Outer {
    @OptIn(kotlin.contracts.ExperimentalContracts::class)
    inline fun <T, R> T.myLet(block: (T) -> R): R {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
            returnsResultOf(block)
        }
        return block(this)
    }
}
