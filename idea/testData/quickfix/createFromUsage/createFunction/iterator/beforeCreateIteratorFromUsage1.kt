// "Create function 'iterator' from usage" "true"
class Foo<T>
fun foo() {
    for (i: Int in Foo<caret><Int>()) { }
}
