fun test() {
    JTest.samTest(SAM { s, n -> s + " " })
    JTest.samTest(SAM { s: Any, n: Int -> x + " " })
}