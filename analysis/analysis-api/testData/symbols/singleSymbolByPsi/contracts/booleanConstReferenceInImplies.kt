// WITH_STDLIB
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun booleanConstReferenceInImplies(): Boolean {
    contr<caret>act {
        returns(true) implies true
    }
    return true
}
