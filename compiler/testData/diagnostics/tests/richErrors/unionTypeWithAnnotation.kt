// RUN_PIPELINE_TILL: CODEGEN
// FIR_DUMP
error class Foo

@Target(AnnotationTarget.TYPE)
annotation class Ann

fun foo(): @Ann (String | Foo) = ""
fun foo2(): @Ann String | Foo = ""
fun foo3(): @Ann (@Ann String | Foo) = ""

fun <T> foo4(t: T & Any): @Ann (@Ann T & Any | Foo) = t // annotations on DNNs seem to be lost, not related to union types (KT-90471)
fun foo5(): @Ann (@Ann (suspend () -> Unit) | Foo) = suspend {}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, stringLiteral */
