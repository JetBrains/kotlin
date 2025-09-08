// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtNamedFunction
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun checkIsString(x: Any): Boolean {
    contract {
        returns(true) implies (x is String)
        returns(false) implies (x !is String)
    }
    return x is String
}
