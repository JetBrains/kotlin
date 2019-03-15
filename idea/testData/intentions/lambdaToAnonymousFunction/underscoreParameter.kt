// IS_APPLICABLE: false
fun foo(f: (Int, Int) -> String) {}

fun test() {
    foo <caret>{ i, _ -> "" }
}