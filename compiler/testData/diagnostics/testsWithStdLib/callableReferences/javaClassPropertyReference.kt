// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KTLC-375, KT-81931
// LANGUAGE_FEATURE_TOGGLED: ForbidJavaClassPropertyReferences

fun test(x: Any) {
    val a = x::<!JAVA_CLASS_PROPERTY_REFERENCE_ERROR!>javaClass<!>
    val b = Any()::<!JAVA_CLASS_PROPERTY_REFERENCE_ERROR!>javaClass<!>
    val c = 2::<!JAVA_CLASS_PROPERTY_REFERENCE_ERROR!>javaClass<!>
    val d = x::<!JAVA_CLASS_PROPERTY_REFERENCE_ERROR!>javaClass<!>.get()

    val direct = x.javaClass
    val literal = Any::class.java
}

/* GENERATED_FIR_TAGS: callableReference, classReference, functionDeclaration, integerLiteral, localProperty,
propertyDeclaration */
