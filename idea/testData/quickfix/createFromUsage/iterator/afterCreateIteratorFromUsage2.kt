// "Create method 'iterator' from usage" "true"
class Foo<T> {
    fun iterator(): Iterator<String> {
        throw Exception("not implemented") //To change body of created methods use File | Settings | File Templates.
    }
}
fun foo() {
    for (i in Foo<Int>()) {
        bar(i)
    }
}
fun bar(i: String) { }
