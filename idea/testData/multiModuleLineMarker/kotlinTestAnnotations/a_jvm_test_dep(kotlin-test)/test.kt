import kotlin.test.*

class <lineMarker descr="Run Test">SimpleTest</lineMarker> {
    @Test fun <lineMarker descr="Run Test">testFoo</lineMarker>() {
        // Will run
    }

    @Ignore fun testFooWrong() {
        // Will not run
    }
}

@Ignore
class <lineMarker descr="Run Test">TestTest</lineMarker> {
    @Test fun <lineMarker descr="Run Test">emptyTest</lineMarker>() {
        // Will not run
    }
}
