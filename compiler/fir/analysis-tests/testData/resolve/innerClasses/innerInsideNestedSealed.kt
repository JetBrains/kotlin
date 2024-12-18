// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73347

class Graph {
    sealed class <!REDECLARATION!>Node<!> {
        inner class Start : <!FINAL_SUPERTYPE, INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Node<!>()
    }

    inner class <!REDECLARATION!>Node<!>
}
