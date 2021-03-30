actual sealed class <!LINE_MARKER("descr='Is subclassed by PlatformAYImplTestClass'")!>TestClass<!> actual constructor() {}
class PlatformAYImplTestClass: TestClass()

fun checkCommonAY(t: TestClass): Int = when (t) {
    is CommonImplTestClass -> 0
    is CommonAImplTestClass -> 1
    is PlatformAYImplTestClass -> 2
}
