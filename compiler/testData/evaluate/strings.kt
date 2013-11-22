package test

enum class MyEnum {
    A
}

// val prop1: "2"
val prop1 = "${1 + 1}"

// val prop2: null
val prop2 = "myEnum=${MyEnum.A}"

// val prop3: "1"
val prop3 = "${1}"

// val prop4: null
val prop4 = "${null}"

// val prop5: "1.0"
val prop5 = "${1.toFloat()}"

// val prop6: "1.0"
val prop6 = "${1.0}"

// val prop7: null
val prop7 = "${javaClass<Int>()}"
