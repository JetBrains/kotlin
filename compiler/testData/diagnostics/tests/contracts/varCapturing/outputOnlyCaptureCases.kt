// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun testReassignmentInCurrentLambda() {
    var stable = ""
    Thread {
        stable = "hello"
        println(stable)
        stable = "2"
    }
    println(stable)
}

private fun testOutputOnlyUninitializedVar(a: Int, v: Any) = run {
    var oldVersionedValue: String? = null
    var added : Boolean
    val aevtPrime = Thread {
        when {
            oldVersionedValue == null -> run { added = true }
            oldVersionedValue == "hi" -> run { added = false }
            else -> run { added = true }
        }
    }
}

private fun testUnstableNotCaptured() {
    Thread {
        var isEmpty = true
        Thread {
            isEmpty = false
        }
        if (isEmpty) {
            println("Empty")
        }
    }
}

/* GENERATED_FIR_TAGS: assignment, equalityExpression, functionDeclaration, functionalType, ifExpression, lambdaLiteral,
localProperty, nullableType, propertyDeclaration, smartcast, stringLiteral, whenExpression */
