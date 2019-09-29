class TestClassWithPrimaryAndSecondaryCtors() {
    var intProp: Int = 42
    var stringProp: String = "42"

    constructor(intProp: Int) : this() {
        this.intProp = intProp
        stringProp = intProp.toString()
    }

    constructor(stringProp: String) : this() {
        this.stringProp = stringProp
    }
}

fun box(): String {
    val testClassPrimary = TestClassWithPrimaryAndSecondaryCtors()
    val testClassSecondaryInt = TestClassWithPrimaryAndSecondaryCtors(777)
    val testClassSecondaryString = TestClassWithPrimaryAndSecondaryCtors("13")
    when {
        testClassPrimary.intProp != 42 -> return "FAIL"
        testClassPrimary.stringProp != "42" -> return "FAIL"
        testClassSecondaryInt.intProp != 777 -> return "FAIL"
        testClassSecondaryInt.stringProp != "777" -> return "FAIL"
        testClassSecondaryString.intProp != 42 -> return "FAIL"
        testClassSecondaryString.stringProp != "13" -> return "FAIL"
        else -> return "OK"
    }
}