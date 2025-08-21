// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ReadDeserializedContracts +UseCallsInPlaceEffect
// See KT-17479

class Test {
    val str: String
    init {
        run {
            this@Test.str = "A"
        }

        run {
            // Not sure do we need diagnostic also here
            this@Test.<!VAL_REASSIGNMENT!>str<!> = "B"
        }

        <!VAL_REASSIGNMENT!>str<!> = "C"
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, init, lambdaLiteral, propertyDeclaration, stringLiteral,
thisExpression */
