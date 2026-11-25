// RUN_PIPELINE_TILL: CODEGEN
// IDE_MODE
// ISSUE: KT-89593

class Context

class C {
    companion object {
        context(_: Context)
        val withContext: C
            get() = C()

        val noContext: C
            get() = C()
    }
}

fun foo(p: C) {}

context(_: Context)
fun test() {
    foo(C.withContext)
    foo(<!DEBUG_INFO_CSR_MIGHT_BE_USED!>C.noContext<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, functionDeclarationWithContext, getter,
objectDeclaration, propertyDeclaration, propertyDeclarationWithContext */
