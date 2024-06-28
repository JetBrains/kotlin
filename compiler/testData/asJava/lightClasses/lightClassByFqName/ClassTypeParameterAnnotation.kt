// one.C
package one

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Anno

class C<@Anno T>