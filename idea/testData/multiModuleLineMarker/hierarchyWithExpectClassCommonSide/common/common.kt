package test

open class <!LINE_MARKER("descr='Is subclassed by ExpectedChild [common] ExpectedChild [jvm] ExpectedChildChild ExpectedChildChildJvm SimpleChild'")!>SimpleParent<!> {
    open fun <!LINE_MARKER("descr='Is overridden in test.ExpectedChild test.ExpectedChildChild test.ExpectedChildChildJvm test.SimpleChild'")!>foo<!>(n: Int) {}
    open val <!LINE_MARKER("descr='Is overridden in test.ExpectedChild test.ExpectedChildChild test.ExpectedChildChildJvm test.SimpleChild'")!>bar<!>: Int get() = 1
}

expect open class <!LINE_MARKER("descr='Is subclassed by ExpectedChildChild ExpectedChildChildJvm'"), LINE_MARKER("descr='Has actuals in JVM'")!>ExpectedChild<!> : SimpleParent {
    override fun <!LINE_MARKER("descr='Overrides function in 'SimpleParent''"), LINE_MARKER("descr='Has actuals in JVM'"), LINE_MARKER("descr='Is overridden in test.ExpectedChildChild test.ExpectedChildChildJvm'")!>foo<!>(n: Int)
    override val <!LINE_MARKER("descr='Overrides property in 'SimpleParent''"), LINE_MARKER("descr='Has actuals in JVM'"), LINE_MARKER("descr='Is overridden in test.ExpectedChildChild test.ExpectedChildChildJvm'")!>bar<!>: Int
}

class ExpectedChildChild : ExpectedChild() {
    override fun <!LINE_MARKER("descr='Overrides function in 'ExpectedChild''")!>foo<!>(n: Int) {}
    override val <!LINE_MARKER("descr='Overrides property in 'ExpectedChild''")!>bar<!>: Int get() = 1
}

class SimpleChild : SimpleParent() {
    override fun <!LINE_MARKER("descr='Overrides function in 'SimpleParent''")!>foo<!>(n: Int) {}
    override val <!LINE_MARKER("descr='Overrides property in 'SimpleParent''")!>bar<!>: Int get() = 1
}