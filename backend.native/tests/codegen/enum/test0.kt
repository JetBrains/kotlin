val TOP_LEVEL = 5

enum class MyEnum(value: Int) {
    VALUE(TOP_LEVEL)
}

fun main(args: Array<String>) {
    println(MyEnum.VALUE.toString())
}
