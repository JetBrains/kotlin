// WITH_STDLIB
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun boolenExprContract(foo: Any?, bar: Any?): Boolean {
    contr<caret>act {
        returns(true) implies (foo == null && bar != null)
    }
    return foo == null && bar != null
}
