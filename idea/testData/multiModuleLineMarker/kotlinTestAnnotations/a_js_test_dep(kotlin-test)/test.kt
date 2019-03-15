import kotlin.test.*

class SimpleTest {
    @Test fun testFoo() {
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
