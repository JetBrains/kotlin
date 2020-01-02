package sample

import kotlin.test.Test
import kotlin.test.assertTrue

class <lineMarker descr="Run Test">SampleTests</lineMarker> {
    @Test
    fun <lineMarker descr="Run Test">testMe</lineMarker>() {
        assertTrue(Sample().checkMe() > 0)
    }
}