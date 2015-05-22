// IS_APPLICABLE: false
fun foo() {
    bar(<caret>{ it })
}

fun bar(b: (Int) -> Int, option: Int = 0) { }
