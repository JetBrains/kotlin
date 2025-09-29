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

fun bar(x: B.() -> Unit) {}

fun A.test() {
    bar {
        <!DSL_SCOPE_VIOLATION!>a<!>()
        this@test.a()
        b()
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, integerLiteral, lambdaLiteral, thisExpression, typeWithExtension */
