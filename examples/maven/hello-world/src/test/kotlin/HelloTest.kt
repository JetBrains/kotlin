package hello.tests

import junit.framework.TestCase
import kotlin.test.assertEquals
import hello.getHelloString

class HelloTest : TestCase() {
    fun testAssert() : Unit {
        assertEquals("Hello, world!", getHelloString())
    }
}
