// ACTION_CLASS: org.jetbrains.kotlin.idea.actions.generate.KotlinGenerateTestSupportActionBase$TearDown
// CONFIGURE_LIBRARY: JUnit@lib/junit-4.12.jar
// TEST_FRAMEWORK: JUnit4
import org.junit.After

class A {<caret>
    @After
    fun tearDown() {
        throw UnsupportedOperationException()
    }
}