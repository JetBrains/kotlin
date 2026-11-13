// IGNORE_STANDALONE
// `LLStandaloneFirElementByPsiElementChooser` crashes on building FIR type references with `createEmptySession()`
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtNamedFunction

class Check {
    fun pets(vararg arguments: Check.(Int) -> Unit) {
    }
}
