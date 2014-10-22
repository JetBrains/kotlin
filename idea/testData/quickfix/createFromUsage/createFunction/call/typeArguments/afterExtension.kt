// "Create function 'foo' from usage" "true"

class A<T>(val items: List<T>) {
    fun test(): Int {
        return items.foo<T, Int, String>(2, "2")
    }
}

fun <E, T, T1> List<E>.foo(arg: T, arg1: T1): T {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
}