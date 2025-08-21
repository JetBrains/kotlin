// LANGUAGE: +ContextReceivers
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtNamedFunction
context(A)
public inline fun <A> myContextOf(): A = this@A
