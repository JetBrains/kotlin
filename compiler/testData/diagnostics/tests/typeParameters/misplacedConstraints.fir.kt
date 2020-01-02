class Foo<T : Cloneable> where T : Comparable<T> {
    fun <U : Cloneable> foo(u: U): U where U: Comparable<U> {
        fun <T: Any> bar() where T: U {}
        return u
    }

    val <U : Cloneable> U.foo: U? where U: Comparable<U>
       get() { return null }
}

class Bar<T : Cloneable, U> where U: Comparable<T> {

}