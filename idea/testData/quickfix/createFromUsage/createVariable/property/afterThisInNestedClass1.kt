// "Create property 'foo'" "true"
// ERROR: Property must be initialized or be abstract

class A<T>(val n: T) {
    inner class B<U>(val m: U) {
        private val foo: A<Int>

        fun test(): A<Int> {
            return this.foo
        }
    }
}
