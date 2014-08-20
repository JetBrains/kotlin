// IS_APPLICABLE: true
fun foo() {
    bar <caret>@l{ it * 3 }
}

fun bar(b: (Int) -> Int) {
    b(42)
}
