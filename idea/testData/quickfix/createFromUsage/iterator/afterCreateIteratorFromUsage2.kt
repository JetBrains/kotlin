// "Create function 'iterator' from usage" "true"
class Foo<T> {
    fun iterator(): Iterator<String> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
fun foo() {
    for (i in Foo<Int>()) {
        bar(i)
    }
}
fun bar(i: String) { }
