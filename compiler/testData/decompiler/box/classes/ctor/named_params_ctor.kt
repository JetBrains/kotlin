class TestClass(val namedString: String, val namedInt: Int) {
}

fun box(): String {
    val testClassOrderedWoutNames = TestClass("String", 4)
    val testClassReorderedWNames = TestClass(namedInt = 4, namedString = "String")
    if (testClassReorderedWNames.namedInt == testClassOrderedWoutNames.namedInt) {
        return "OK"
    } else {
        return "FAIL"
    }
}