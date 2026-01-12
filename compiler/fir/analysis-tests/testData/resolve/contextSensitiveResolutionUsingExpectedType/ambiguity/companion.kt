// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// RENDER_DIAGNOSTICS_FULL_TEXT

package foo

enum class MyEnum {
    A, B
}

class Test {
    companion object {
        val A = Any()
    }

    fun test1wrong(x: MyEnum): Boolean {
        return x == <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>A<!>
    }

    fun test1enum(x: MyEnum): Boolean {
        return x == MyEnum.A
    }

    fun test1companion(x: MyEnum): Boolean {
        return x == Companion.A
    }

    fun test2wrong(x: MyEnum): Int = <!NO_ELSE_IN_WHEN!>when<!> (x) {
        <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>A<!> -> 1
        B -> 2
    }

    fun test2enum(x: MyEnum): Int = <!WHEN_ON_SEALED!>when (x) {
        MyEnum.A -> 1
        B -> 2
    }<!>

    fun test2companion(x: MyEnum): Int = <!NO_ELSE_IN_WHEN!>when<!> (x) {
        Companion.A -> 1
        B -> 2
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, enumDeclaration, enumEntry, equalityExpression,
functionDeclaration, integerLiteral, objectDeclaration, propertyDeclaration, whenExpression, whenWithSubject */
