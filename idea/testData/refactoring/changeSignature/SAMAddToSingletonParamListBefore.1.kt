fun test() {
    JTest.samTest(SAM { s -> s + " " })
    JTest.samTest(SAM { s -> s + " " })
    JTest.samTest(SAM { it + " " })
}