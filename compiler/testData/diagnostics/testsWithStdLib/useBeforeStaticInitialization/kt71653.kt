// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
<!POSSIBLE_INITIALIZATION_DEADLOCK!>object Table1<!> {
    val reference = Table2
}

<!POSSIBLE_INITIALIZATION_DEADLOCK!>object Table2<!> {
    val reference = Table1
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, assignment, checkNotNullCall, classDeclaration, companionObject,
functionDeclaration, functionalType, lambdaLiteral, localProperty, nullableType, objectDeclaration, override,
primaryConstructor, propertyDeclaration, safeCall, suspend, thisExpression, typeParameter */
