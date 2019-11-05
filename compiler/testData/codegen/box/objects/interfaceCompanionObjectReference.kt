// !LANGUAGE: +NestedClassesInAnnotations
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

import kotlin.test.*

interface Test {
    companion object {
        val x = "O"

        val y1 = Test.x

        val y2 = 42.let { x }

        val y3: String
        init {
            fun localFun() = x
            y3 = localFun()
        }

        fun method() = x
        val y4 = method()

        val anonObject = object {
            override fun toString() = x
        }
        val y5 = anonObject.toString()

        init {
            assertEquals(x, y1)
            assertEquals(x, y2)
            assertEquals(x, y3)
            assertEquals(x, y4)
            assertEquals(x, y5)
        }
    }
}

annotation class Anno {
    companion object {
        val x = "K"

        val y1 = Anno.x

        val y2 = 42.let { x }

        val y3: String
        init {
            fun localFun() = x
            y3 = localFun()
        }

        fun method() = x
        val y4 = method()

        val anonObject = object {
            override fun toString() = x
        }
        val y5 = anonObject.toString()

        init {
            assertEquals(x, y1)
            assertEquals(x, y2)
            assertEquals(x, y3)
            assertEquals(x, y4)
            assertEquals(x, y5)
        }
    }
}

fun box() = Test.x + Anno.x
