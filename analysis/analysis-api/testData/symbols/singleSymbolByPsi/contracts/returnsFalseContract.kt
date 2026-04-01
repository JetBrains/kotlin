// WITH_STDLIB
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun returnsFalseContract(foo: Any?): Boolean {
    contr<caret>act {
        returns(false) implies (foo != null)
    }
    return foo == null
}