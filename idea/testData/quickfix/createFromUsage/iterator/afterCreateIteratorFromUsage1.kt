// "Create method 'iterator' from usage" "true"
class Foo<T> {
    fun iterator(): Iterator<T> {
        throw Exception("not implemented") //To change body of created methods use File | Settings | File Templates.
    }
}
fun foo() {
    for (i: Int in Foo<Int>()) { }
}
