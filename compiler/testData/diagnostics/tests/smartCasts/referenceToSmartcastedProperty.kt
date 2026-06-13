// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78429

val x: Any = ""

fun test() {
    if (x is String) {
        val ref = ::x
        ref.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

/* GENERATED_FIR_TAGS: callableReference, intersectionType, isExpression, propertyDeclaration, smartcast */
