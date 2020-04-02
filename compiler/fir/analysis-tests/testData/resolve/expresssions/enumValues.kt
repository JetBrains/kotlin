enum class MyEnum {
    FIRST,
    SECOND,
    LAST;

    fun bar() = 42
}

fun foo() {
    val values = MyEnum.values()

    for (value in values) {
        value.bar()
    }

    val first = MyEnum.valueOf("FIRST")
    val last = MyEnum.valueOf("LAST")
}