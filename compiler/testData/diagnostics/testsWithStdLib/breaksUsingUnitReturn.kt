// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTIC_ARGUMENTS
fun test() {
    listOf("a", "b").forEach {
        return@forEach
        <!UNIT_RETURN_AS_BREAK("forEach")!>return<!>
    }

    run {
        setOf(1, 2).takeWhile {
            if (it < 3) <!UNIT_RETURN_AS_BREAK("takeWhile")!>return@run<!>
            true
        }
    }

    mapOf("a" to 1, "b" to 2).filter {
        run {
            return@run
        }
        true
    }
}

/* GENERATED_FIR_TAGS: comparisonExpression, functionDeclaration, ifExpression, integerLiteral, lambdaLiteral,
stringLiteral */
