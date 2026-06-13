// RUN_PIPELINE_TILL: FRONTEND
fun <T> bar(): T {
    return null <!UNCHECKED_CAST!>as T<!>
}

class X() : <!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>B<!> by <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!><!VARIABLE_EXPECTED!><!UNRESOLVED_REFERENCE!>get<!>()<!> = <!CANNOT_INFER_PARAMETER_TYPE!>bar<!>()<!> {
    val prop = <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!><!VARIABLE_EXPECTED!><!CANNOT_INFER_PARAMETER_TYPE!>bar<!>()<!> = 2<!>
}

/* GENERATED_FIR_TAGS: asExpression, assignment, classDeclaration, functionDeclaration, inheritanceDelegation,
integerLiteral, nullableType, primaryConstructor, propertyDeclaration, typeParameter */
