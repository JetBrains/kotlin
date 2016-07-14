// "Create member function 'Foo.component3'" "true"
class Foo<T> {
    operator fun component1(): Int { return 0 }
    operator fun component2(): Int { return 0 }
}
fun foo() {
    val (a, b, c) = Foo<caret><Int>()
}
