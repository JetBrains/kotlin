// "Create method 'component3' from usage" "true"
class Foo<T> {
    fun component1(): Int { return 0 }
    fun component2(): Int { return 0 }
    fun component3(): String {
        throw Exception("not implemented") //To change body of created methods use File | Settings | File Templates.
    }
}
fun foo() {
    val (a, b, c: String) = Foo<Int>()
}
