// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

@DslMarker
@Target(AnnotationTarget.TYPE)
annotation class Ann

class A {
    fun a() = 1
}

class B {
    fun b() = 2
}

fun foo(x: (@Ann A).() -> Unit) {}
fun bar(x: (@Ann B).() -> Unit) {}

fun testOnDispatchReceivers() {
    foo {
        ::a
        bar {
            ::<!DSL_SCOPE_VIOLATION!>a<!>
            this@foo::a
            ::b
        }
    }
}

@DslMarker
annotation <!DSL_MARKER_WITH_DEFAULT_TARGETS!>class Ann2<!>

@Ann2
class C

fun C.c() = 1

@Ann2
class D

fun D.d() = 2

fun foo2(x: C.() -> Unit) {}
fun bar2(x: D.() -> Unit) {}

fun testOnExtensionReceivers() {
    foo2 {
        ::c
        bar2 {
            ::<!DSL_SCOPE_VIOLATION!>c<!>
            this@foo2::c
            ::d
        }
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, callableReference, classDeclaration, funWithExtensionReceiver,
functionDeclaration, functionalType, integerLiteral, lambdaLiteral, thisExpression, typeWithExtension */
