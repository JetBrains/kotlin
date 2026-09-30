// RESOLVE_FILE
@file:Anno(i = fun foo() = 1)

package one

@Target(AnnotationTarget.FILE)
annotation class Anno(val i: Int)
