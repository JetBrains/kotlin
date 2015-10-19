// ALLOW_AST_ACCESS

package test

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class A

class SimpleTypeParameterAnnotation {
    fun <@A T> foo(x: T) {}
}
