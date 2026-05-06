// RUN_PIPELINE_TILL: BACKEND
class TestInitValInLambdaCalledOnce {
    val x: Int
    init {
        1.run {
            x = 0
        }
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, init, integerLiteral, lambdaLiteral, propertyDeclaration */
