// RUN_PIPELINE_TILL: FRONTEND

class C {
    companion object {
        val x = 42
    }
}

typealias CA = C

val test1 = CA.<!UNRESOLVED_REFERENCE!>Companion<!>
val test2 = CA.<!UNRESOLVED_REFERENCE!>Companion<!>.x