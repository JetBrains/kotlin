actual sealed class <!LINE_MARKER("descr='Is subclassed by PlatformAXImplTestClass'")!>TestClass<!> actual constructor() {}
class PlatformAXImplTestClass: TestClass()

fun checkCommonAX(t: TestClass): Int = when (t) {
    is CommonImplTestClass -> 0
    is CommonAImplTestClass -> 1
    is PlatformAXImplTestClass -> 2
}
