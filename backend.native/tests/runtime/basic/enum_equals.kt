package runtime.basic.enum_equals

import kotlin.test.*

enum class EnumA {
    A, B
}

enum class EnumB {
    B
}

@Test fun run() {
    println(EnumA.A == EnumA.A)
    println(EnumA.A == EnumA.B)
    println(EnumA.A == EnumB.B)
}