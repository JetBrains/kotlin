actual typealias Expect = String

interface Derived : Base {
    override fun expectInReturnType(): Expect

    override fun expectInArgument(e: Expect)

    override fun Expect.expectInReceiver()

    override val expectVal: <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>Expect<!>

    override var expectVar: <!VAR_TYPE_MISMATCH_ON_OVERRIDE!>Expect<!>
}
