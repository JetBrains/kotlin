// one.C
package one

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Anno

class C {
    val <@Anno T> T.foo get() = 1
}
