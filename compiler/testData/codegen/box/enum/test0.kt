// WITH_STDLIB

import kotlin.test.*

val TOP_LEVEL = 5

enum class MyEnum(value: Int) {
    VALUE(TOP_LEVEL)
}

fun box(): String {
    assertEquals("VALUE", MyEnum.VALUE.toString())

    return "OK"
}
