// "Create function 'iterator' from usage" "true"
class Foo<T> {
    fun iterator(): Iterator<T> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
fun foo() {
    for (i: Int in Foo<Int>()) { }
}
