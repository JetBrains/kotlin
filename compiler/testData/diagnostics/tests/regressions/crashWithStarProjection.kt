// RUN_PIPELINE_TILL: FRONTEND
class A<T : Function1<*, Any>>(var x: T) {
    val y = A(<!TYPE_MISMATCH, TYPE_MISMATCH, TYPE_MISMATCH!>fun(<!EXPECTED_PARAMETER_TYPE_MISMATCH!>x: Any<!>): Any = 1<!>)
}

/* GENERATED_FIR_TAGS: anonymousFunction, classDeclaration, integerLiteral, nullableType, primaryConstructor,
propertyDeclaration, starProjection, typeConstraint, typeParameter */
