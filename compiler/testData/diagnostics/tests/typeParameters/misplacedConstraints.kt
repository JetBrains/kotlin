class Foo<<!MISPLACED_TYPE_PARAMETER_CONSTRAINTS!>T : Cloneable<!>> where T : Comparable<T> {
    fun <<!MISPLACED_TYPE_PARAMETER_CONSTRAINTS!>U : Cloneable<!>> foo(u: U): U where U: Comparable<U> {
        fun <<!BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER, MISPLACED_TYPE_PARAMETER_CONSTRAINTS!>T: Any<!>> bar() where T: U {}
        return u
    }

    val <<!MISPLACED_TYPE_PARAMETER_CONSTRAINTS!>U : Cloneable<!>> U.foo: U? where U: Comparable<U>
       get() { return null }
}

class Bar<T : Cloneable, U> where U: Comparable<T> {

}