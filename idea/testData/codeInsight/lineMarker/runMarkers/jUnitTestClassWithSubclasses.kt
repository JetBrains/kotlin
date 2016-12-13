// CONFIGURE_LIBRARY: JUnit@lib/junit-4.12.jar
package testing

import junit.framework.TestCase
import org.junit.Test

abstract class <lineMarker descr="*"><lineMarker descr="Run Test">KBase</lineMarker></lineMarker> : TestCase() {
    @Test
    fun testFoo() {

    }
}

class <lineMarker descr="*">KTest</lineMarker> : KBase() {
    @Test
    fun <lineMarker descr="*">testBar</lineMarker>() {

    }
}

class <lineMarker descr="*">KTest2</lineMarker> : KBase() {
    @Test
    fun <lineMarker descr="*">testBaz</lineMarker>() {

    }
}