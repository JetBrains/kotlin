// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// http://youtrack.jetbrains.net/issue/KT-413

open class A {
    fun f() {}
}

class B : A() {
    fun g() {
        super<!UNEXPECTED_SAFE_CALL!>?.<!>f()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, safeCall, superExpression */
