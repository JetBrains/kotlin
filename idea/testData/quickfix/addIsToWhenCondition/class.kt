// "Add 'is' before 'Foo'" "true"

class Foo

fun test(a: Any) {
    when (a) {
        <caret>Foo -> {}
    }
}