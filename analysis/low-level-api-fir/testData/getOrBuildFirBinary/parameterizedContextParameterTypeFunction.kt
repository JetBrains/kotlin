// IGNORE_STANDALONE
// KT-91917
// `LLStandaloneFirElementByPsiElementChooser` doesn't count context parameters of function types correctly
// LANGUAGE: +ContextParameters
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtNamedFunction

public inline fun <T, R> myContext(with: T, block: context(T) () -> R): R {
    return block(with)
}
