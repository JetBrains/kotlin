abstract class A

fun foo(i: Int) {}

<!VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class B(val i: Int) : @Suppress(<!ERROR_SUPPRESSION!>"VALUE_CLASS_CANNOT_EXTEND_CLASSES"<!>) A() {
    @Suppress(<!ERROR_SUPPRESSION!>"SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_VALUE_CLASS"<!>)
    constructor() : this(42) {
        foo(i)
    }

    @Suppress(<!ERROR_SUPPRESSION!>"ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS"<!>)
    abstract val y: Int
}

interface C {
    @Suppress(<!ERROR_SUPPRESSION!>"PRIVATE_PROPERTY_IN_INTERFACE"<!>)
    private val x: Int

    @Suppress(<!ERROR_SUPPRESSION!>"METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE"<!>)
    override fun hashCode() = 42
}

<!MUST_BE_INITIALIZED!>@Suppress(<!ERROR_SUPPRESSION!>"PROPERTY_WITH_NO_TYPE_NO_INITIALIZER"<!>)
val z<!>
