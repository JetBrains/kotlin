// RUN_PIPELINE_TILL: FRONTEND
// INFERENCE_HELPERS
class A : <!UNRESOLVED_REFERENCE!>Undefined<!>(<!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!CANNOT_INFER_PARAMETER_TYPE!>materialize<!>()))

/* GENERATED_FIR_TAGS: capturedType, checkNotNullCall, classDeclaration, functionDeclaration, integerLiteral,
nullableType, outProjection, typeParameter, vararg */
