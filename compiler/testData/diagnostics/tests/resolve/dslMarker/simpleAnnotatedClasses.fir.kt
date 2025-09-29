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

fun foo(x: A.() -> Unit) {}
fun bar(x: B.() -> Unit) {}

fun test() {
    foo {
        a()
        bar {
            <!DSL_SCOPE_VIOLATION!>a<!>()
            this@foo.a()
            b()
        }
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, functionalType, integerLiteral,
lambdaLiteral, thisExpression, typeWithExtension */
