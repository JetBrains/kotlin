// "Create member function 'iterator'" "true"
class Foo<T>
fun foo() {
    for (i in Foo<caret><Int>()) {
        bar(i)
    }
}
fun bar(i: String) { }
