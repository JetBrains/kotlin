// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTIC_ARGUMENTS
fun test() {
    val x = run {
        listOf("a", "b").takeWhile {
            if (it == "b") <!MATCHING_TYPE_RETURN_AS_BREAK("takeWhile")!>return@run emptyList()<!>
            return@takeWhile true
        }
    }

    val y = run {
        setOf(1, 2).takeWhile { outer ->
            listOf("a", "b").forEach {
                if (outer == 2) <!MATCHING_TYPE_RETURN_AS_BREAK("takeWhile")!>return@run emptyList()<!>
            }
            true
        }
    }

    val z = mapOf("a" to 1, "b" to 2).filter {
        run {
            return@run true
        }
    }

    val w = emptyList<Int>().fold("") { acc, i ->
        fun nestedFun(): String {
            return "test"
        }
        acc + nestedFun() + i
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, comparisonExpression, equalityExpression, functionDeclaration, ifExpression,
integerLiteral, lambdaLiteral, localFunction, localProperty, propertyDeclaration, stringLiteral */
