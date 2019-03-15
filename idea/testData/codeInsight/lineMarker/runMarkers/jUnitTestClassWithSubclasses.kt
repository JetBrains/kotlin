// CONFIGURE_LIBRARY: JUnit@lib/junit-4.12.jar
package testing

import junit.framework.TestCase
import org.junit.Test

abstract class <lineMarker descr="*"><lineMarker descr="Run Test">KBase</lineMarker></lineMarker> : TestCase() {
    // NOTE: this differs from Java tooling behaviour, see KT-27977
    @Test
    fun <lineMarker descr="Run Test">testFoo</lineMarker>() {

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

abstract class <lineMarker descr="*"><lineMarker descr="Run Test">AbstractClassWithoutInheritors</lineMarker></lineMarker> : TestCase() {
    // NOTE: showing line markers for abstract method, which has no inheritors is not ideal, because those methods cannot actually be run
    // Sadly, run configurations can actually be created for them (same in Java), so this behaviour is consistent with context menu
    @Test
    fun <lineMarker descr="Run Test">testFoo</lineMarker>() {

    }
}
