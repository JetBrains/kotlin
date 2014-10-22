// "Create function 'foo' from usage" "true"

class B<T>(val t: T) {
    fun <T1> foo(i: Int, arg: T1): Int {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

class A<T>(val b: B<T>) {
    fun test(): Int {
        return b.foo<String>(2, "2")
    }
}