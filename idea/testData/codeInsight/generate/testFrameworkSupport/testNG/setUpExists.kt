// ACTION_CLASS: org.jetbrains.kotlin.idea.actions.generate.KotlinGenerateTestSupportActionBase$SetUp
// NOT_APPLICABLE
// CONFIGURE_LIBRARY: TestNG@testng(-|[0-9]|\.)*jar
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

@Test class A {<caret>
    @BeforeMethod
    fun setUp() {
        throw UnsupportedOperationException()
    }
}