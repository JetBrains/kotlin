fun test(b: Boolean) {
    var fn: () -> String

    <caret>try {
        fn = { "foo" }
    } catch (e: Exception) {
        fn = { "bar" }
    }
}