import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
context(a: String?)
fun vali<caret>date(param: Int?) {
    contract {
        returns() implies (a != null)
    }
    a!!
}
