// FIR_IDENTICAL

annotation class TestAnn(val x: String)

@TestAnn("testVal.property")
val testVal: String = ""
