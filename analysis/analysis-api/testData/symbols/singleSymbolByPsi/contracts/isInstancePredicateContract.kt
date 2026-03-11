// WITH_STDLIB
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
