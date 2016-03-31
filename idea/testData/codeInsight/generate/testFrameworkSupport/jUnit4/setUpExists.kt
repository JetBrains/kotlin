// ACTION_CLASS: org.jetbrains.kotlin.idea.actions.generate.KotlinGenerateTestSupportActionBase$SetUp
// CONFIGURE_LIBRARY: JUnit@lib/junit-4.11.jar
import org.junit.Before

class A {<caret>
    @Before
    fun setUp() {
        throw UnsupportedOperationException()
    }
}