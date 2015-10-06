class Foo<<!MISPLACED_TYPE_PARAMETER_CONSTRAITS!>T : Cloneable<!>> where T : Comparable<T> {
    fun <<!MISPLACED_TYPE_PARAMETER_CONSTRAITS!>U : Cloneable<!>> foo(u: U): U where U: Comparable<U> {
        return u
    }

    val <<!MISPLACED_TYPE_PARAMETER_CONSTRAITS!>U : Cloneable<!>> U.foo: U? where U: Comparable<U>
       get() { return null }
}

class Bar<T : Cloneable, U> where U: Comparable<T> {

}