// ATTACH_DUPLICATE_STDLIB

fun test(text: String) {
    <caret>consume(text)
}

fun consume(text: String) {}