// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
data class A<!DATA_CLASS_WITHOUT_PARAMETERS!>()<!>

fun foo(a: A) {
    a.<!UNRESOLVED_REFERENCE!>component1<!>()
    a.<!UNRESOLVED_REFERENCE!>component2<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, primaryConstructor */
