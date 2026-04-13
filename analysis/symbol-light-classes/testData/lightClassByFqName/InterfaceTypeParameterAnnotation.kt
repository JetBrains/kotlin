// one.C
package one

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Anno

interface C<@Anno T>