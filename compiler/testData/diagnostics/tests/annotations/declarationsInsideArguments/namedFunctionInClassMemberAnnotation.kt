// RUN_PIPELINE_TILL: FRONTEND
// WITH_EXTRA_CHECKERS
// ISSUE: KT-77041

annotation class Anno(val i: Int)

class Outer {
    @Anno(i = fun <!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() = 1)
    class Nested

    @Anno(i = fun <!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() = 1)
    inner class Inner

    @Anno(i = fun <!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() = 1)
    object Obj
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, inner, integerLiteral,
localFunction, nestedClass, objectDeclaration, primaryConstructor, propertyDeclaration */
