// RUN_PIPELINE_TILL: BACKEND
class Some(var foo: Int) {
    init {
        if (foo < 0) {
            foo = 0
        }
    }

    val y = run {
        foo = 1
        foo
    }

    constructor(): this(-1) {
        foo = 2
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, comparisonExpression, ifExpression, init, integerLiteral,
lambdaLiteral, primaryConstructor, propertyDeclaration, secondaryConstructor */
