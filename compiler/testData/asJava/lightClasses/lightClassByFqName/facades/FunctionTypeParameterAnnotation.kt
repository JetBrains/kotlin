// one.FunctionTypeParameterAnnotationKt
package one

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Anno

fun <@Anno T> foo(t: T) {

}
