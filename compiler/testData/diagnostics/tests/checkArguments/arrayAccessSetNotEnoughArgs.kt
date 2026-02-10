// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

class A {
    operator fun set(x: String, y: Boolean, value: Int) {}

    fun d(x: Int) {
        <!NO_VALUE_FOR_PARAMETER!>this[""] = 1<!>
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, integerLiteral, operator, stringLiteral,
thisExpression */
