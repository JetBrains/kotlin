fun test(b: Boolean) {
    var fn: () -> String

    <caret>when (b) {
        true -> fn = { "foo" }
        else -> fn = { "bar" }
    }
}