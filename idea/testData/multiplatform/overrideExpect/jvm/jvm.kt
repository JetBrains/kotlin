actual typealias Expect = String

interface Derived : Base {
    override fun expectInReturnType(): Expect

    override fun expectInArgument(e: Expect)

    override fun Expect.expectInReceiver()

    override val expectVal: Expect

    override var expectVar: Expect
}
