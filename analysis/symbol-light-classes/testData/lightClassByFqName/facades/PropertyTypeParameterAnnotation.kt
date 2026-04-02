// one.PropertyTypeParameterAnnotationKt
package one

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Anno

val <@Anno T> T.foo get() = 1
