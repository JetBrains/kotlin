// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE
val x get() = null
val <!IMPLICIT_NOTHING_PROPERTY_TYPE!>y<!> get() = null!!

fun foo() {
    <!DEBUG_INFO_CONSTANT!>x<!> checkType { _<Nothing?>() }
    y <!UNREACHABLE_CODE!>checkType { _<Nothing>() }<!>
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
getter, infix, lambdaLiteral, nullableType, propertyDeclaration, typeParameter, typeWithExtension */
