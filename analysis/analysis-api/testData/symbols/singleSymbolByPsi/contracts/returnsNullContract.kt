// WITH_STDLIB
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun returnsNullContract(foo: Any?): Any? {
    contr<caret>act {
        returns(null) implies (foo == null)
    }
    return foo
}
