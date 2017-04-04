import kotlin.test.*
import kotlin.properties.*

class IdentityEqualsIsUsedToUnescapeLazyValTest {
    var equalsCalled = 0
    private val a by lazy { ClassWithCustomEquality { equalsCalled++ } }

    fun doTest() {
        a
        a
        assertTrue(equalsCalled == 0, "fail: equals called $equalsCalled times.")
    }
}

private class ClassWithCustomEquality(private val onEqualsCalled: () -> Unit) {
    override fun equals(other: Any?): Boolean {
        onEqualsCalled()
        return super.equals(other)
    }
}

fun box() {
    IdentityEqualsIsUsedToUnescapeLazyValTest().doTest()
}