// ACTION_CLASS: org.jetbrains.kotlin.idea.actions.generate.KotlinGenerateTestSupportActionBase$SetUp
// CONFIGURE_LIBRARY: TestNG@plugins/testng/lib/testng.jar
import org.testng.annotations.Test

open class A {
    open fun setUp() {

    }
}

@Test class B : A() {<caret>

}