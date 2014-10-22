// "Create function 'foo' from usage" "true"

class B<T>(val t: T) {
    fun <T1, T2> foo(arg: T1, arg1: T2): T1 {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

class A<T>(val b: B<T>) {
    fun test(): Int {
        return b.foo<Int, String>(2, "2")
    }
}