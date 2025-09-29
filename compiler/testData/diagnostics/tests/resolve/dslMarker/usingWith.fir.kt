// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
@DslMarker
annotation <!DSL_MARKER_WITH_DEFAULT_TARGETS!>class Ann<!>

@Ann
class A {
    fun a() = 1
}

@Ann
class B {
    fun b() = 2
}

fun test(a: A, b: B) {
    with(a) l1@{
        a()
        with(b) {
            <!DSL_SCOPE_VIOLATION!>a<!>()
            this@l1.a()
            b()
        }
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, integerLiteral, lambdaLiteral,
thisExpression */
