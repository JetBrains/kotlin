package one.two

@Target(AnnotationTarget.TYPE)
annotation class Anno(val i: Int)

const val constant = ""

class MyClass<A>

data class MyDataClass(val pr<caret>op: @Anno(0 + constant) MyClass<@Anno(1 + constant) MyClass<@Anno(2 + constant) Int>>) {
    class MyClass<B>

    companion object {
        const val constant = 0
    }
}
