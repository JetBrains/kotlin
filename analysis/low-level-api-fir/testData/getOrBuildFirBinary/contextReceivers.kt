// LANGUAGE: +ContextReceivers
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: C

context(String)
class C {
    constructor() {}

    context(Int)
    constructor(a: Any) {}

    context(String) fun foo() {}
    context(String) val bar: String get() = ""
}