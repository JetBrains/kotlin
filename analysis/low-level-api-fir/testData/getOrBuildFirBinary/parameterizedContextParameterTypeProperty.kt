// IGNORE_STANDALONE
// KT-91917
// `LLStandaloneFirElementByPsiElementChooser` doesn't count context parameters of function types correctly
// LANGUAGE: +ContextParameters
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtProperty

public inline val <T> T.myContext: context(T) () -> Unit get() = null!!
