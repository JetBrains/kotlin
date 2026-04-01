// WITH_STDLIB
import java.lang.IllegalStateException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun Boolean.referenceReceiverInContract() {
    contr<caret>act {
        returns() implies this@referenceReceiverInContract
    }
    if (!this) throw IllegalStateException()
}
