package codegen.enum.test0

import kotlin.test.*

val TOP_LEVEL = 5

enum class MyEnum(value: Int) {
    VALUE(TOP_LEVEL)
}

@Test fun runTest() {
    println(MyEnum.VALUE.toString())
}
