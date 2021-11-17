fun x() {
    1.toCall(x.a<caret>v)
}

infix fun Int.toCall(y: String): Char = 'a'

