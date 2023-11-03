// MEMBER_NAME_FILTER: prop
package one.two

@Target(AnnotationTarget.TYPE)
annotation class Anno(val i: Int)

const val constant = ""

class MyClass<A>

data class MyDa<caret>taClass(val prop: @Anno(0 + constant) MyClass<@Anno(1 + constant) MyClass<@Anno(2 + constant) Int>>) {
    class MyClass<B>

    companion object {
        const val constant = 0
    }
}
