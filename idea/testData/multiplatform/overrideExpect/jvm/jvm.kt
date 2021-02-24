actual typealias <!LINE_MARKER("descr='Has declaration in common module'")!>Expect<!> = String

interface Derived : Base {
    override fun <!LINE_MARKER("descr='Overrides function in 'Base''")!>expectInReturnType<!>(): Expect

    override fun <!LINE_MARKER("descr='Overrides function in 'Base''")!>expectInArgument<!>(e: Expect)

    override fun Expect.<!LINE_MARKER("descr='Overrides function in 'Base''")!>expectInReceiver<!>()

    override val <!LINE_MARKER("descr='Overrides property in 'Base''")!>expectVal<!>: Expect

    override var <!LINE_MARKER("descr='Overrides property in 'Base''")!>expectVar<!>: Expect
}
