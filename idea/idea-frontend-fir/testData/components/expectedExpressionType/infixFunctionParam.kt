fun x() {
    1 toCall a<caret>v
}

infix fun Int.toCall(y: String): Char = 'a'

