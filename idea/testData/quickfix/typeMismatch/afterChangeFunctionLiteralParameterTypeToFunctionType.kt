// "Change type from 'String' to '(Int) -> String'" "true"
fun foo(f: ((Int) -> String) -> String) {
    foo {
        (f: (Int) -> String<caret>) -> f(42)
    }
}