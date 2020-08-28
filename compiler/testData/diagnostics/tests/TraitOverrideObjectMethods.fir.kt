// JAVAC_EXPECTED_FILE
interface MyTrait: <!INTERFACE_WITH_SUPERCLASS!>Object<!> {
    override fun toString(): String
    public override fun finalize()
    public override fun wait()
}
