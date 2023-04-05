// LANGUAGE: +MultiPlatformProjects
// !DIAGNOSTICS: -NO_ACTUAL_FOR_EXPECT, -ACTUAL_WITHOUT_EXPECT
// FIR_IDENTICAL
class SimpleClass {
    protected fun foo() = Unit
}

expect class ExpClass {
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> fun foo()

    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> val bar: Int
}

actual class ActClass {
    actual <!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> fun foo() = Unit

    actual <!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> val bar: Int = 42
}

expect open class ExpOpenClass {
    protected fun foo()
}

actual open class ActOpenClass {
    actual protected fun foo() = Unit
}

enum class SimpleEnum {
    ENTRY;

    protected fun foo() = Unit
}

expect enum class ExpEnumClass {
    ENTRY;

    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> fun foo()

    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> val bar: Int
}

actual enum class ActEnumClass {
    ENTRY;

    actual <!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> fun foo() = Unit

    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> val bar: Int = 42
}
