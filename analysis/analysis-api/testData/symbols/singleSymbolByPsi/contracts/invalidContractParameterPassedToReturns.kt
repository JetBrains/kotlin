// WITH_STDLIB
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun invalidContract(foo: Any, bar: Boolean) {
    cont<caret>ract {
        returns(foo) implies bar
    }
    return if (bar) foo else null
}
