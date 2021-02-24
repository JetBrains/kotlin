package test

actual open class <!LINE_MARKER("descr='Is subclassed by ExpectedChildChildJvm'"), LINE_MARKER("descr='Has declaration in common module'")!>ExpectedChild<!> : SimpleParent() {
    actual override fun <!LINE_MARKER("descr='Overrides function in 'SimpleParent''"), LINE_MARKER("descr='Has declaration in common module'"), LINE_MARKER("descr='Is overridden in test.ExpectedChildChildJvm'")!>foo<!>(n: Int) {}
    actual override val <!LINE_MARKER("descr='Overrides property in 'SimpleParent''"), LINE_MARKER("descr='Has declaration in common module'"), LINE_MARKER("descr='Is overridden in test.ExpectedChildChildJvm'")!>bar<!>: Int get() = 1
}

class ExpectedChildChildJvm : ExpectedChild() {
    override fun <!LINE_MARKER("descr='Overrides function in 'ExpectedChild''")!>foo<!>(n: Int) {}
    override val <!LINE_MARKER("descr='Overrides property in 'ExpectedChild''")!>bar<!>: Int get() = 1
}