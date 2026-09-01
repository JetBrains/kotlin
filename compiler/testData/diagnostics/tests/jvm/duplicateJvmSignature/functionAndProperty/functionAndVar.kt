// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

class C {
    <!CONFLICTING_JVM_DECLARATIONS!>fun setX(x: Int)<!> {}

    var x: Int = 1
        <!CONFLICTING_JVM_DECLARATIONS!>set(v)<!> {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration, setter */
