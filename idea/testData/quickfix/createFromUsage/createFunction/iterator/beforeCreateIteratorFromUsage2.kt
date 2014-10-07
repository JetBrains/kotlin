// "Create function 'iterator' from usage" "true"
class Foo<T>
fun foo() {
    for (i in Foo<caret><Int>()) {
        bar(i)
    }
}
fun bar(i: String) { }
