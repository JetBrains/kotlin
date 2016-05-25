// CONFIGURE_LIBRARY: JUnit@lib/junit-4.12.jar
package testing

import junit.framework.TestCase
import org.junit.Test

abstract class <lineMarker descr="Run Test"><lineMarker descr="<html><body>Is subclassed by<br>&nbsp;&nbsp;&nbsp;&nbsp;<a href="#javaClass/testing.KTest">testing.KTest</a><br>&nbsp;&nbsp;&nbsp;&nbsp;<a href="#javaClass/testing.KTest2">testing.KTest2</a><br><div style='margin-top: 5px'><font size='2'>Click or press ⌥⌘B to navigate</font></div></body></html>"></lineMarker>KBase</lineMarker> : TestCase() {
    @Test
    fun testFoo() {

    }
}

class <lineMarker descr="Run Test">KTest</lineMarker> : KBase() {
    @Test
    fun <lineMarker descr="Run Test">testBar</lineMarker>() {

    }
}

class <lineMarker descr="Run Test">KTest2</lineMarker> : KBase() {
    @Test
    fun <lineMarker descr="Run Test">testBaz</lineMarker>() {

    }
}