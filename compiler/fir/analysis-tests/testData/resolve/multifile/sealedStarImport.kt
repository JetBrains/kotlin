// FILE: test.kt

package test

sealed class Test {
    object O : Test()

    class Extra(val x: Int): Test
}

// FILE: main.kt

package other

import test.Test.*

abstract class Factory {
    abstract fun createTest(): Test

    abstract fun createObj(): O

    abstract fun createExtra(): Extra
}