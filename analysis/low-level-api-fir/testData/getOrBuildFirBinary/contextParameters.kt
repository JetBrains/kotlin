// LANGUAGE: +ContextReceivers, +ContextParameters
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: C

context(_: String)
class C {
    constructor() {}

    context(s: String)
    constructor(a: Any) {}

    context(s: String) fun foo() {}
    context(s: String) val bar: String get() = ""
}