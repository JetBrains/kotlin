// RUN_PIPELINE_TILL: BACKEND
class Test(protected var prop1: Int = 1) {
    protected var prop2: Int = 2

    private fun test() {
        prop1 = 3
        prop2 = 4
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, integerLiteral, primaryConstructor,
propertyDeclaration */
