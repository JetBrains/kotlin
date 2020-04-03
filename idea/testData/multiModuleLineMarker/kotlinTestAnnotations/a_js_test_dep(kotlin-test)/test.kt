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
class TestTest {
    @Test fun emptyTest() {
        // Will not run
    }
}
