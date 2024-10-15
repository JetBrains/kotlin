// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
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