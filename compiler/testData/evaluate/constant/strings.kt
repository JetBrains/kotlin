package test

enum class MyEnum {
    A
}

// val prop1: \"2\"
val prop1 = "${1 + 1}"

// val prop2: null
val prop2 = "myEnum=${MyEnum.A}"

// val prop3: \"1\"
val prop3 = "${1}"

// val prop4: \"null\"
val prop4 = "${null}"

// val prop5: \"1.0\"
val prop5 = "${1.toFloat()}"

// val prop6: \"1.0\"
val prop6 = "${1.0}"

// val prop7: null
val prop7 = "${Int::class.java}"

// val prop8: \"a1.0\"
val prop8 = "a${1.toDouble()}"

// val prop9: \"ab\"
val prop9 = "a" + "b"

// val prop10: \"abb\"
val prop10 = prop9 + "b"

// val prop11: 6
val prop11 = "kotlin".length
