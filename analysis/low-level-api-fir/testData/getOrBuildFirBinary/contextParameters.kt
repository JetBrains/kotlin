// LANGUAGE: +ContextParameters
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: C

class C {
    context(s: String) fun foo() {}
    context(s: String) val bar: String get() = ""
}