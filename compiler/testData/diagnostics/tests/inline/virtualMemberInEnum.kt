// RUN_PIPELINE_TILL: FRONTEND
enum class Some {
    A {
        override fun foo(s: () -> String): String {
            return s() + s()
        }
    };

    //SHOULD BE ERROR REPORTED
    open <!DECLARATION_CANT_BE_INLINED!>inline<!> fun foo(s: () -> String): String {
        return s()
    }
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, functionalType, inline */
