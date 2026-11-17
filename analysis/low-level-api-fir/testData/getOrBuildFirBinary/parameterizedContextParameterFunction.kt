// LANGUAGE: +ContextParameters
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtNamedFunction

context(context: A)
public inline fun <A> myContextOf(): A = context
