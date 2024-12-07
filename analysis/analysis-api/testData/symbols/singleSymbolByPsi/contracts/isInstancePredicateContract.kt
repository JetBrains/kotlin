// WITH_STDLIB
// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class Foo

@OptIn(ExperimentalContracts::class)
fun isInstancePredicateContract(value: Any) {
    contr<caret>act {
        returns() implies (value is Foo)
    }
    if (value !is Foo) {
        throw IllegalStateException()
    }
}
