// LANGUAGE: +ContextReceivers
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
class Foo<A> {
    context(A)
    constructor()
}
