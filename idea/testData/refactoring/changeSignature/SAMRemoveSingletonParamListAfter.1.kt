fun test() {
    JTest.samTest(SAM { s + " " })
    JTest.samTest(SAM { it + " " })
}