// MEMBER_NAME_FILTER: component1
package one.two

@Target(AnnotationTarget.TYPE)
annotation class Anno(val i: Int)

const val constant = ""

class MyClass<A>

data class MyDa<caret>taClass(
    val prop1: @Anno(0 + constant) MyClass<@Anno(1 + constant) MyClass<@Anno(2 + constant) Int>>,
    val prop2: @Anno(3 + constant) MyClass<@Anno(4 + constant) MyClass<@Anno(5 + constant) Int>>,
) {
    class MyClass<B>

    companion object {
        const val constant = 0
    }
}
