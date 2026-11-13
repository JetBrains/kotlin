// IGNORE_STANDALONE
// `LLStandaloneFirElementByPsiElementChooser` crashes on building FIR type references with `createEmptySession()`
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtNamedFunction
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun Any?.myRequireNotNull() {
    contract {
        returns() implies (this@myRequireNotNull != null)
    }
    if (this == null) throw IllegalStateException()
}
