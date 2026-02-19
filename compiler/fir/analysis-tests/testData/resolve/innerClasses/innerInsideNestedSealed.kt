// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73347

class Graph {
    sealed class <!REDECLARATION!>Node<!> {
        inner class Start : <!FINAL_SUPERTYPE, INACCESSIBLE_OUTER_CLASS_RECEIVER!>Node<!>()
    }

    inner class <!REDECLARATION!>Node<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, inner, nestedClass, sealed */
