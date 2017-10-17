package codegen.objectExpression.expr1

import kotlin.test.*

@Test fun runTest() {
    val a = "a"

    val x = object {
        override fun toString(): String {
            return foo(a) + foo("b")
        }

        fun foo(s: String) = s + s
    }

    println(x.toString())
}