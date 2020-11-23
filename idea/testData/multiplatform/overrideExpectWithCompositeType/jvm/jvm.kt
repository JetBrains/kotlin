actual typealias Expect = String

interface Derived : Base {
    override fun expectInReturnType(): Box<Expect>

    override fun expectInArgument(e: Box<Expect>)

    override fun Box<Expect>.expectInReceiver()

    override val expectVal: Box<Expect>

    override var expectVar: Box<Expect>
}
