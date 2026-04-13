// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79017
class Clazz {
    private class <!REDECLARATION!>Private1<!>
    private class <!REDECLARATION!>Private1<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, nestedClass */
