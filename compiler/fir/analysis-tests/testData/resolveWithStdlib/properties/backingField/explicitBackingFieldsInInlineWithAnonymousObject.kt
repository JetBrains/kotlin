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

inline fun publicTestInArg(noinline arg: () -> MutableList<Int> = { numbers }) {}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, inline, override,
propertyDeclaration, smartcast */
