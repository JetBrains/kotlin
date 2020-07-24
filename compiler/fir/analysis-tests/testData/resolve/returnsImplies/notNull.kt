import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun foo(x: String?, y: String?, z: () -> Unit): Any? {
    contract {
        returns(true) implies (y != null && x != null)
        callsInPlace(z, InvocationKind.AT_MOST_ONCE)
    }

    if(y == null){
        val result = x != null
        return result
    }
    return true
}