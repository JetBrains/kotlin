// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// ISSUE: KT-73667

@DslMarker
@Target(AnnotationTarget.TYPE)
annotation class Dsl

class A
class B

val invokeExtensionOnA: A.() -> Unit = {}

fun foo(x: @Dsl A.() -> Unit) {}
fun bar(x: @Dsl B.() -> Unit) {}

fun main() {
    foo {
        bar {
            <!DSL_SCOPE_VIOLATION!>invokeExtensionOnA<!>() // Correct DSL_SCOPE_VIOLATION error in K1, no error in K2
        }
    }
}
