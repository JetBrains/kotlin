// CONFIGURE_LIBRARY: TestNG@plugins/testng/lib/testng.jar
package testing

import org.testng.annotations.Test

abstract class <lineMarker></lineMarker>KBase {
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