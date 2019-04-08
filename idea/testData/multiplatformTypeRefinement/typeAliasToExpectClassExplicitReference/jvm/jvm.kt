actual class A {
    actual fun commonMember() { }

    fun platformMember() { }
}

fun test() {
    <!RESOLUTION_TO_CLASSIFIER("TypealiasFromCommon", "EXPECT_CLASS_AS_FUNCTION", "Expected class TypealiasFromCommon does not have default constructor")!>TypealiasFromCommon<!>().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>commonMember<!>()
    <!RESOLUTION_TO_CLASSIFIER("TypealiasFromCommon", "EXPECT_CLASS_AS_FUNCTION", "Expected class TypealiasFromCommon does not have default constructor")!>TypealiasFromCommon<!>().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>platformwMember<!>()
}