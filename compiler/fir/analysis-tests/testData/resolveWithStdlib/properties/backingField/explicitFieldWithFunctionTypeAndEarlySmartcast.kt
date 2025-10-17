// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-80418

class Test {
    val a: Any
        field: () -> Unit = {}

    fun usage1() {
        //a
        a()
    }

    fun usage2() {
        a
        a()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, explicitBackingField, functionDeclaration, functionalType, lambdaLiteral,
propertyDeclaration, smartcast */
