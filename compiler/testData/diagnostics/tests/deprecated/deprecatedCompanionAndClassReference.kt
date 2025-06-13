// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-54209

class A {
    @Deprecated("Deprecated companion")
    companion object
}


fun test() {
    A::class
    A.<!DEPRECATION!>Companion<!>::class
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, companionObject, functionDeclaration, objectDeclaration,
stringLiteral */
