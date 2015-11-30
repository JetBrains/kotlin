// "Create function 'foo'" "true"

fun <T> run(f: () -> T) = f()

fun test() {
    run { <caret>foo() }
}