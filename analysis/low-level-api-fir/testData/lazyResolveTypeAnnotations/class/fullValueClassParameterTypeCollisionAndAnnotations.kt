// LANGUAGE: +FullValueClasses
package one.two

@Target(AnnotationTarget.TYPE)
annotation class Anno(val i: Int)

const val constant = ""

class MyClass<A>

value class MyValueClass(
    val pr<caret>op: @Anno(0 + constant) MyClass<@Anno(1 + constant) MyClass<@Anno(2 + constant) Int>>,
    val second: @Anno(3 + constant) MyClass<Int>,
) {
    class MyClass<B>

    companion object {
        const val constant = 0
    }
}
