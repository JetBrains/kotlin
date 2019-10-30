// "Add parameter to constructor 'Foo'" "true"
// WITH_RUNTIME

class Foo

fun test(name: String) {
    name.also { Foo(it<caret>) }
}