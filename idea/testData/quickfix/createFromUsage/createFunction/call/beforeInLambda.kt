// "Create function 'foo' from usage" "true"

fun run<T>(f: () -> T) = f()

fun test() {
    run { <caret>foo() }
}