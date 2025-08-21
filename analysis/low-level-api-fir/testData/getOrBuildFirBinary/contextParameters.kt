// LANGUAGE: +ContextParameters
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: C

@Target(AnnotationTarget.TYPE)
annotation class A

class C {
    context(s: String) fun foo() {}
    context(s: String) val bar: String get() = ""
    fun baz(
        l1: context(String) () -> Unit,
        l2: @A context(String) () -> Unit,
        l3: @A (context(String) () -> Unit)?,
        l4: suspend context(String) () -> Unit,
        l5: suspend @A context(String) () -> Unit,
        l6: @A (suspend context(String) () -> Unit)?,
    ) {}
}