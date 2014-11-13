// "Create class 'Foo'" "true"

fun run<T>(f: () -> T) = f()

fun test() {
    run { <caret>Foo() }
}