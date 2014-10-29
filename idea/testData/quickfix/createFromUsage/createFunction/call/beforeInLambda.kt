// "Create function 'foo'" "true"

fun run<T>(f: () -> T) = f()

fun test() {
    run { <caret>foo() }
}