// IGNORE_STANDALONE
// KT-91917
// `LLStandaloneFirElementByPsiElementChooser` doesn't support definitely non-nullable types
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtNamedFunction
class FooBar {
    fun <T> functionWithTAndAnyValue(otherVal: T & Any) {}
}