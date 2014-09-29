// "Create function 'foo' from usage" "true"
// ERROR: Unresolved reference: s

class A<T>(val n: T) {
    fun foo(s: Any, arg: T): T {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun test(): Int {
    return A(1).foo(s, 1)
}