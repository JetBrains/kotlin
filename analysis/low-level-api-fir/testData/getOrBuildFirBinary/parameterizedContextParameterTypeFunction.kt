// IGNORE_STANDALONE
// `LLStandaloneFirElementByPsiElementChooser` crashes on building FIR type references with `createEmptySession()`
// LANGUAGE: +ContextParameters
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtNamedFunction

public inline fun <T, R> myContext(with: T, block: context(T) () -> R): R {
    return block(with)
}
