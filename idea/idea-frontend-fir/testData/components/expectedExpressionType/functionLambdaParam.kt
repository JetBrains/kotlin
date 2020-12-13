fun x() {
    toCall(1, "", a<caret>v)
}

fun toCall(x: Int, y: String, lambda: (Int) -> String): Char = 'a'

// EXPECTED_TYPE: kotlin/Function1<kotlin/Int, kotlin/String>