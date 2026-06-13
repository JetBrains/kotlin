// common.pack.ClassToCheck
// WITH_STDLIB
// MODULE: main-common
// FILE: common.kt
package common.pack

expect class EXPECT

class ClassToCheck {
    fun someFunWithLambda(block: (String) -> Int) {}

    fun someFunWithString(s: String) {}

    class MyException: Throwable()

    val myExpect: EXPECT? = null
}

// MODULE: m1-jvm()()(main-common)
// FILE: jvm.kt
package common.pack

actual class EXPECT
