// RUN_PIPELINE_TILL: FRONTEND
// ALLOW_KOTLIN_PACKAGE
package kotlin

@Deprecated("")
@<!DEPRECATED_SINCE_KOTLIN_WITHOUT_ARGUMENTS!>DeprecatedSinceKotlin<!>
fun foo() {}

fun test() {
    foo()
}