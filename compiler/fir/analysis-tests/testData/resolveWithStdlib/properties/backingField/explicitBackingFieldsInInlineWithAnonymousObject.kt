// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE

val numbers: List<Int>
    field = mutableListOf()

abstract class Foo {
    abstract fun foo(): MutableList<Int>
}

inline fun publicTest() = object : Foo() {
    override fun <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>foo<!>() = numbers
}

inline fun publicTestInArg(noinline arg: () -> MutableList<Int> = { <!RETURN_TYPE_MISMATCH!>numbers<!> }) {}

inline fun outer() {
    val local = object {
        private inline fun foo(noinline block: () -> MutableList<Int> = { <!RETURN_TYPE_MISMATCH!>numbers<!> }) {}
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, explicitBackingField, functionDeclaration,
functionalType, inline, lambdaLiteral, noinline, override, propertyDeclaration, smartcast */
