// LANGUAGE: +MultiPlatformProjects
// RUN_PIPELINE_TILL: FRONTEND

// MODULE: common
// FILE: file1.kt
package example

expect open class A()

sealed class B<out T> : A()
class C<T> : B<T>() {
    fun c() = Unit
}

fun <T> util(a: (T) -> Unit, b: (T) -> Unit, c: (T) -> Unit) = Unit

fun test(x: Any?) {
    util({ it: B<CharSequence> -> }, { it: C<*> -> }, ) { it ->
        if (<!DEBUG_INFO_EXPRESSION_TYPE("example.B<kotlin.CharSequence> & example.C<*>")!>it<!> == x) {
            x.<!UNRESOLVED_REFERENCE!>c<!>()
        }
    }
}

// MODULE: jvm()()(common)
// FILE: file2.kt
package example

actual open class A actual constructor()

/* GENERATED_FIR_TAGS: actual, annotationDeclaration, classDeclaration, classReference, expect, functionDeclaration,
nullableType, operator, override, primaryConstructor, propertyDeclaration, starProjection, typeAliasDeclaration */
