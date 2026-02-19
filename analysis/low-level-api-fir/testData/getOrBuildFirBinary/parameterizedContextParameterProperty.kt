// LANGUAGE: +ContextParameters
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtProperty
context(context: A)
public inline val <A> myContextOf: A get() = context
