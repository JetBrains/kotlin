// CONFIGURE_LIBRARY: JUnit@lib/junit-4.11.jar
package testing

import junit.framework.TestCase
import org.junit.Test

abstract class <lineMarker></lineMarker>KBase : TestCase() {
    @Test
    fun testFoo() {

    }
}

class KTest : KBase() {
    @Test
    fun testBar() {

    }
}

class KTest2 : KBase() {
    @Test
    fun testBaz() {

    }
}