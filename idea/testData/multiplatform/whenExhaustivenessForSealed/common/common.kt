expect sealed class <!LINE_MARKER("descr='Is subclassed by CommonAImplTestClass CommonImplTestClass PlatformAXImplTestClass PlatformAYImplTestClass'")!>TestClass<!>()
class CommonImplTestClass: TestClass()


fun checkCommon(t: TestClass): Int = <!EXPECT_TYPE_IN_WHEN_WITHOUT_ELSE, NO_ELSE_IN_WHEN!>when<!> (t) {
    is CommonImplTestClass -> 0
}
