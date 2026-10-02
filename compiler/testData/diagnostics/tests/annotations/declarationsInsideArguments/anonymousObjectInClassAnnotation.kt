// RUN_PIPELINE_TILL: FRONTEND
// WITH_EXTRA_CHECKERS
// ISSUE: KT-77041

annotation class Anno(val i: Int)

@Anno(i = <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>object {
    fun foo() = 1
    private fun bar() = 2
    <!REDUNDANT_VISIBILITY_MODIFIER!>public<!> val baz = 3
}.foo()<!>)
class Check

/* GENERATED_FIR_TAGS: annotationDeclaration, anonymousObjectExpression, classDeclaration, functionDeclaration,
integerLiteral, primaryConstructor, propertyDeclaration */
