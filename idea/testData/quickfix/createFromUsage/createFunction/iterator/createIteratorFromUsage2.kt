// "Create member function 'Foo.iterator'" "true"
class Foo<T>
fun foo() {
    for (i in Foo<caret><Int>()) {
        bar(i)
    }
}
fun bar(i: String) { }
