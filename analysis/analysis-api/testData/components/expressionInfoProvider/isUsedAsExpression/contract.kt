import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun test(block: () -> Unit) contract [
<expr>callsInPlace(block, InvocationKind.EXACTLY_ONCE)</expr>
] {
    block()
}
