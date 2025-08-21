// LANGUAGE: +ContextReceivers
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtProperty
context(A)
public inline val <A> myContextOf: A get() = this@A
