// WITH_STDLIB
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun returnsNotNullContract(foo: Any?): Any? {
    contr<caret>act {
        returnsNotNull() implies (foo != null)
    }
    return foo
}
