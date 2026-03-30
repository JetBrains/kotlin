// RUN_PIPELINE_TILL: FRONTEND
annotation class Ann

class C {
    fun foo() {
        class Local {
            @Ann<!SYNTAX!><!>
        }
    }

    @Ann<!SYNTAX!><!>
}

@Ann<!SYNTAX!><!>

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, localClass */
