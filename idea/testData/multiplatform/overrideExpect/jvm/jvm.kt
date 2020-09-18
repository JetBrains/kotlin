actual typealias Expect = String

interface Derived : Base {
    override fun expectInReturnType(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>Expect<!>

    override fun expectInArgument(e: Expect)

    override fun Expect.expectInReceiver()
}
