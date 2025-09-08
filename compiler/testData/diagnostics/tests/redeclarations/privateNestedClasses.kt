// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// ISSUE: KT-79017
class Clazz {
    private class <!REDECLARATION!>Private1<!>
    private class <!REDECLARATION!>Private1<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, nestedClass */
