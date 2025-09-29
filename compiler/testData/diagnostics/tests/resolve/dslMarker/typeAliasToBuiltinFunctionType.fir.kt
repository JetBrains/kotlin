// RUN_PIPELINE_TILL: FRONTEND
@DslMarker
annotation <!DSL_MARKER_WITH_DEFAULT_TARGETS!>class MyDsl<!>

@MyDsl
class X

fun x(block: X.() -> Unit) {}

@MyDsl
class A

fun a(block: A.() -> Unit) {}

val useX1: X.() -> Unit = TODO()
typealias FunctionType = X.() -> Unit

val useX2: FunctionType = TODO()


fun test() {
    x {
        a {
            <!DSL_SCOPE_VIOLATION!>useX1<!>()
            <!DSL_SCOPE_VIOLATION!>useX2<!>()
        }
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, functionalType, lambdaLiteral,
propertyDeclaration, typeAliasDeclaration, typeWithExtension */
