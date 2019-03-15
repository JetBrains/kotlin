// JAVAC_EXPECTED_FILE
interface MyTrait: <!INTERFACE_WITH_SUPERCLASS, PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Object<!> {
    override fun toString(): String
    public override fun finalize()
    <!CONFLICTING_INHERITED_JVM_DECLARATIONS!>public <!OVERRIDING_FINAL_MEMBER!>override<!> fun wait()<!>
}
