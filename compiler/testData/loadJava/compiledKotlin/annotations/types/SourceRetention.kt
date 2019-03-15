// ALLOW_AST_ACCESS
// NO_CHECK_SOURCE_VS_BINARY

package test

@Target(AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class A

class TypeParameterAnnotation {
    fun <@A T> foo(x: T) {}
}

@A
typealias TypeAliasAnnotation<X> = List<X>

fun typeAnnotation(): @A Unit {}
