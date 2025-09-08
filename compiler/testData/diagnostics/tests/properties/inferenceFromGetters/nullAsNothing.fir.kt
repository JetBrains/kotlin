// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE
val x get() = null
val <!IMPLICIT_NOTHING_PROPERTY_TYPE!>y<!> get() = null!!

fun foo() {
    x checkType { _<Nothing?>() }
    y checkType { _<Nothing>() }
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
getter, infix, lambdaLiteral, nullableType, propertyDeclaration, typeParameter, typeWithExtension */
