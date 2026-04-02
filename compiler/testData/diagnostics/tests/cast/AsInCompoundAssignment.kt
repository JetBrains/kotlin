// RUN_PIPELINE_TILL: BACKEND
class A {
    var b = 1
}

fun Any.test() {
    (this as A).b += 1 <!INTEGER_LITERAL_CAST_INSTEAD_OF_TO_CALL!>as Int<!>
}

/* GENERATED_FIR_TAGS: additiveExpression, asExpression, assignment, classDeclaration, funWithExtensionReceiver,
functionDeclaration, integerLiteral, localProperty, propertyDeclaration, thisExpression */
