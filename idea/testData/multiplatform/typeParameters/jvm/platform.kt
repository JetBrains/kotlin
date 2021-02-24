@file:Suppress("ACTUAL_WITHOUT_EXPECT")

package foo

interface <!LINE_MARKER("descr='Is implemented by A [jvm] AImpl'")!>B<!>

actual interface <!LINE_MARKER("descr='Is implemented by AImpl'"), LINE_MARKER("descr='Has declaration in common module'")!>A<!> : B {
    actual fun <!LINE_MARKER("descr='Has declaration in common module'"), LINE_MARKER("descr='Is implemented in foo.AImpl'")!>commonFun<!>()

    fun <!LINE_MARKER("descr='Is implemented in foo.AImpl'")!>platformFun<!>()
}

class AImpl : A {
    override fun <!LINE_MARKER("descr='Implements function in 'A''")!>commonFun<!>() {}
    override fun <!LINE_MARKER("descr='Implements function in 'A''")!>platformFun<!>() {}
}

@Suppress("UNUSED_PARAMETER")
fun takeList(inv: List<B>) {}