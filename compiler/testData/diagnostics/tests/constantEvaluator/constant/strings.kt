// WITH_STDLIB
package test

enum class MyEnum {
    A
}

// val prop1: \"2\"
<!DEBUG_INFO_CONSTANT_VALUE("\"2\"")!>val prop1 = "${1 + 1}"<!>

// val prop2: null
<!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop2 = "myEnum=${MyEnum.A}"<!>

// val prop3: \"1\"
<!DEBUG_INFO_CONSTANT_VALUE("\"1\"")!>val prop3 = "${1}"<!>

// val prop4: \"null\"
<!DEBUG_INFO_CONSTANT_VALUE("\"null\"")!>val prop4 = "${null}"<!>

// val prop5: \"1.0\"
<!DEBUG_INFO_CONSTANT_VALUE("\"1.0\"")!>val prop5 = "${1.toFloat()}"<!>

// val prop6: \"1.0\"
<!DEBUG_INFO_CONSTANT_VALUE("\"1.0\"")!>val prop6 = "${1.0}"<!>

// val prop7: null
<!DEBUG_INFO_CONSTANT_VALUE("null")!>val prop7 = "${Int::class.java}"<!>

// val prop8: \"a1.0\"
<!DEBUG_INFO_CONSTANT_VALUE("\"a1.0\"")!>val prop8 = "a${1.toDouble()}"<!>

// val prop9: \"ab\"
<!DEBUG_INFO_CONSTANT_VALUE("\"ab\"")!>val prop9 = "a" + "b"<!>

// val prop10: \"abb\"
<!DEBUG_INFO_CONSTANT_VALUE("\"abb\"")!>val prop10 = prop9 + "b"<!>

// val prop11: 6
<!DEBUG_INFO_CONSTANT_VALUE("6")!>val prop11 = "kotlin".length<!>
