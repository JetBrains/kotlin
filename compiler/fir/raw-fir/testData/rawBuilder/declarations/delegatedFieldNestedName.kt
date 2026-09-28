package second

@Target(AnnotationTarget.TYPE)
annotation class Anno(val i: Int)

interface Base<A> {
    fun foo() {}
}

interface Second<T> {

}

const val outer = 0
const val inner = ""

class MyClass :
    @Anno(0 + outer) Base<@Anno(1 + outer) Base<@Anno(2 + outer) Int>> by Companion,
    @Anno(4 + outer) Second<@Anno(5 + outer) String> by NestedObject {
    companion object : @Anno(6 + inner) Base<@Anno(7 + inner) Base<@Anno(8 + inner) Int>> {
        const val outer = ""
        const val inner = 0
    }

    object NestedObject : @Anno(9 + inner) Second<@Anno(10 + inner) String>
}
