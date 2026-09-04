// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

class C {
    <!CONFLICTING_JVM_DECLARATIONS!>fun getX(t: Any)<!> = 1
    val Any.x: Int
        <!CONFLICTING_JVM_DECLARATIONS!>get()<!> = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, integerLiteral, propertyDeclaration,
propertyWithExtensionReceiver */
