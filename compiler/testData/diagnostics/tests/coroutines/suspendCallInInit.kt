// RUN_PIPELINE_TILL: FRONTEND
suspend fun foo() {}

suspend fun test() {
    class Foo {
        init {
            <!NON_LOCAL_SUSPENSION_POINT!>foo<!>()
        }

        val prop = <!NON_LOCAL_SUSPENSION_POINT!>foo<!>()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, init, localClass, propertyDeclaration, suspend */
