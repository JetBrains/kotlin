// RUN_PIPELINE_TILL: BACKEND
// WITH_EXPERIMENTAL_CHECKERS
// WITH_EXTRA_CHECKERS

class Test {
    var number: Int = 10
}

fun Test.foo() {
    val number = 20
    this.number = number + 1
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, funWithExtensionReceiver, functionDeclaration,
integerLiteral, localProperty, propertyDeclaration, thisExpression */
