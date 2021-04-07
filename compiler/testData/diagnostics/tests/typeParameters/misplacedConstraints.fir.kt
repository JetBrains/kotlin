class Foo<T : Cloneable> where T : Comparable<T> {
    fun <U : Cloneable> foo(u: U): U where U: Comparable<U> {
        fun <<!BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER!>T: Any<!>> bar() where T: U {}
        return u
    }

    val <U : Cloneable> U.foo: U? where U: Comparable<U>
       get() { return null }
}

class Bar<T : Cloneable, U> where U: Comparable<T> {

}
