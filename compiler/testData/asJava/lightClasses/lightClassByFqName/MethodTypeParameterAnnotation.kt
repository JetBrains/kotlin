// one.C
package one

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Anno

class C {
    fun <@Anno T> foo(t: T) {

    }
}
