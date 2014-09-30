// "Create property 'foo' from usage" "true"
// ERROR: Property must be initialized or be abstract

class A<T>(val n: T) {
    val foo: A<Int>

    inner class B<U>(val m: U) {
        fun test(): A<Int> {
            return this@A.foo
        }
    }
}
