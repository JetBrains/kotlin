import test.doGenerateParamAssertions as C

class TestString : C<String>() {
    override fun doTest(s: String) { }
}

class TestUnit : C<Unit>() {
    override fun doTest(s: Unit) { }
}

fun doTest() {
    C.runTest(TestString())
    C.runTest(TestUnit())
}
