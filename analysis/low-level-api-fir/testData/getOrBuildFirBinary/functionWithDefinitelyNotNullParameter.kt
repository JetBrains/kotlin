// IGNORE_STANDALONE
// `LLStandaloneFirElementByPsiElementChooser` crashes on building FIR type references with `createEmptySession()`
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtNamedFunction
class FooBar {
    fun <T> functionWithTAndAnyValue(otherVal: T & Any) {}
}