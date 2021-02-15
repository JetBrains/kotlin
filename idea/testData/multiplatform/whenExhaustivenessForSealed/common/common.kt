expect sealed class <!LINE_MARKER("descr='Is subclassed by CommonAImplTestClass CommonImplTestClass PlatformAXImplTestClass PlatformAYImplTestClass'")!>TestClass<!>()
class CommonImplTestClass: TestClass()


fun checkCommon(t: TestClass): Int = when (t) {
    is CommonImplTestClass -> 0
}
