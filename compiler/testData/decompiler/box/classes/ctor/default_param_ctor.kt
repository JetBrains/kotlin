class TestClass(val paramDef: String = "DEFAULT") {
}

fun box(): String {
    val testClassDef = TestClass()
    val testClassNonDef = TestClass("NON_DEF")
    if (testClassDef.paramDef != testClassNonDef.paramDef) {
        return "OK"
    } else {
        return "FAIL"
    }
}