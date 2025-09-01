// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
package test

enum class Sample {
    FIRST, SECOND, THIRD;
}

fun trivial(s: Sample): Int {
    return when (s) {
        FIRST -> 1
        SECOND -> 2
        THIRD -> 3
    }
}

fun shouldNotWork(s: Sample): Int {
    return when {
        s == FIRST -> 1
        s == SECOND -> 2
        s == THIRD -> 3
        else -> 0
    }
}

class Container {
    val SECOND = test.Sample.SECOND

    fun priority(s: Sample): Int {
        val FIRST = test.Sample.THIRD
        return when (s) {
            FIRST -> 3
            <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>SECOND<!> -> 2
            test.Sample.FIRST -> 1
            else -> 0
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, equalityExpression, functionDeclaration,
integerLiteral, localProperty, propertyDeclaration, smartcast, whenExpression, whenWithSubject */
