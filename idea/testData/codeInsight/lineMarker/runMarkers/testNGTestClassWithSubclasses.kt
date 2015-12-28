// CONFIGURE_LIBRARY: TestNG@plugins/testng/lib/testng.jar
package testing

import org.testng.annotations.Test

abstract class <lineMarker descr="*"><lineMarker descr="*"></lineMarker>KBase</lineMarker> {
    @Test
    fun <lineMarker descr="*">testFoo</lineMarker>() {

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