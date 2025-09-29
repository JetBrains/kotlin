// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-60383

@DslMarker
annotation <!DSL_MARKER_WITH_DEFAULT_TARGETS!>class Ann<!>

@Ann
class A

fun A.f(x: Short): Short = x
fun A.f(x: Long): Long = x

@Ann
class B

fun foo(x: A.() -> Unit) {}
fun bar(x: B.() -> Unit) {}

fun test() {
    foo {
        bar {
            <!NONE_APPLICABLE!>f<!>(1)
        }
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, integerLiteral, lambdaLiteral, typeWithExtension */
