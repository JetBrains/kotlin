// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: -CollectionLiterals
// LANGUAGE_FEATURE_TOGGLED: CollectionLiteralsBasedAnnotationResolution

// FILE: a.kt
package behaviorChanged

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class Bar(val foo: Foo)
annotation class Foo(val arr: Array<String> = [])

fun main() {
    fun Foo(lst: List<String>) = behaviorChanged.Foo()
    @Bar(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>Foo(<!UNSUPPORTED_FEATURE!>[]<!>)<!>)
    42
}

// FILE: b.kt
package alreadyBroken

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class Bar(val foo: Foo)
annotation class Foo()

fun main() {
    fun Foo() = alreadyBroken.Foo()
    @Bar(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>Foo()<!>)
    42
}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, integerLiteral, localFunction, primaryConstructor,
propertyDeclaration */
