// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

class Test {
    val typeFunctionalCheck: (Int) -> Unit
        <!REDUNDANT_EXPLICIT_BACKING_FIELD!>field<!>: Int.() -> Unit = {}

    val typeFunctionalCheck2: context(Int) () -> Unit
        <!REDUNDANT_EXPLICIT_BACKING_FIELD!>field<!>: (Int) -> Unit = {}

    val typeFunctionalCheck3: context(Int) () -> Unit
        <!REDUNDANT_EXPLICIT_BACKING_FIELD!>field<!>: Int.() -> Unit = {}

    fun usage() {
        typeFunctionalCheck(1)
        typeFunctionalCheck2(1)
        typeFunctionalCheck3(1)

        with(1){
            typeFunctionalCheck<!NO_VALUE_FOR_PARAMETER!>()<!>
            typeFunctionalCheck2()
            typeFunctionalCheck3()
        }
    }
}

fun usageOutside() {
    Test().typeFunctionalCheck(1)
    Test().typeFunctionalCheck2(1)
    Test().typeFunctionalCheck3(1)

    with(1){
        <!NO_VALUE_FOR_PARAMETER!>Test().typeFunctionalCheck()<!>
        Test().typeFunctionalCheck2()
        Test().typeFunctionalCheck3()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, explicitBackingField, functionDeclaration, functionalType, integerLiteral,
lambdaLiteral, propertyDeclaration, typeWithContext, typeWithExtension */
