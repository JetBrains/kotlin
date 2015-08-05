//ALLOW_AST_ACCESS
package test

target(AnnotationTarget.TYPE)
annotation class A

fun @A String.foo() {}
