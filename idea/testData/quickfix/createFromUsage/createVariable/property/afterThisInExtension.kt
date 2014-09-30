// "Create property 'foo' from usage" "true"
// ERROR: Property must be initialized or be abstract

class A<T>(val n: T) {
    val foo: A<Int>

}

fun <U> A<U>.test(): A<Int> {
    return this.foo
}
