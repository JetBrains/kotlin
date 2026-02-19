// LANGUAGE: +ContextParameters
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtProperty

public inline val <T> T.myContext: context(T) () -> Unit get() = null!!
