// "Change type from 'String' to 'Int'" "true"
fun foo(f: (Int) -> String) {
    foo {
        (x: Int) -> ""
    }
}