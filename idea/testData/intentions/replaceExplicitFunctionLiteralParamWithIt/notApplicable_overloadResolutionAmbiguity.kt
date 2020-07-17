// IS_APPLICABLE: false
fun test() {
    foo { <caret>i -> i + 1 }
}

fun foo(f: (Int) -> Int) {}
fun foo(f: (Int, Int) -> Int) {}