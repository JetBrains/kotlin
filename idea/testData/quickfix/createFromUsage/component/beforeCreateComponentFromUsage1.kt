// "Create function 'component3' from usage" "true"
class Foo<T> {
    fun component1(): Int { return 0 }
    fun component2(): Int { return 0 }
}
fun foo() {
    val (a, b, c) = Foo<caret><Int>()
}
