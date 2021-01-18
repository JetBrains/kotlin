actual typealias <!LINE_MARKER("descr='Has declaration in common module'")!>Expect<!> = String

interface Derived : Base {
    override fun <!LINE_MARKER("descr='Overrides function in 'Base''")!>expectInReturnType<!>(): Box<Expect>

    override fun <!LINE_MARKER("descr='Overrides function in 'Base''")!>expectInArgument<!>(e: Box<Expect>)

    override fun Box<Expect>.<!LINE_MARKER("descr='Overrides function in 'Base''")!>expectInReceiver<!>()

    override val <!LINE_MARKER("descr='Overrides property in 'Base''")!>expectVal<!>: Box<Expect>

    override var <!LINE_MARKER("descr='Overrides property in 'Base''")!>expectVar<!>: Box<Expect>
}
