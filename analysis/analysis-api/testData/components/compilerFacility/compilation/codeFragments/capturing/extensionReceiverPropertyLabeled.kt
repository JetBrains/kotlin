fun test() {
    1.ext
}

val Int.ext: String
    get() {
        <caret>return "foo"
    }