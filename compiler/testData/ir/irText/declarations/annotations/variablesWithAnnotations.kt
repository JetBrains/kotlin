// FIR_IDENTICAL

annotation class TestAnn(val x: String)

fun foo() {
    @TestAnn("foo/testVal")
    val testVal = "testVal"

    @TestAnn("foo/testVar")
    var testVar = "testVar"
}