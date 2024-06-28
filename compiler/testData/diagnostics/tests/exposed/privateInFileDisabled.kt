// LANGUAGE: -PrivateInFileEffectiveVisibility

class Public {
    private open class NestedPrivate

    fun <!EXPOSED_FUNCTION_RETURN_TYPE!>test1<!>() = NestedPrivate()
    fun test2(<!EXPOSED_PARAMETER_TYPE!>p: NestedPrivate<!>) {}
    fun <!EXPOSED_RECEIVER_TYPE!>NestedPrivate<!>.test3() {}
    val <!EXPOSED_PROPERTY_TYPE!>test4<!> = NestedPrivate()
    class Test5 : <!EXPOSED_SUPER_CLASS!>NestedPrivate()<!>
}

private class PrivateInFileClass {
    private open class NestedPrivate

    fun <!EXPOSED_FROM_PRIVATE_IN_FILE!>test1<!>() = NestedPrivate()
    fun test2(<!EXPOSED_FROM_PRIVATE_IN_FILE!>p: NestedPrivate<!>) {}
    fun <!EXPOSED_FROM_PRIVATE_IN_FILE!>NestedPrivate<!>.test3() {}
    val <!EXPOSED_FROM_PRIVATE_IN_FILE!>test4<!> = NestedPrivate()
    class Test5 : <!EXPOSED_FROM_PRIVATE_IN_FILE!>NestedPrivate()<!>
}

private interface PrivateInFile {
    private class Private

    fun <!EXPOSED_FROM_PRIVATE_IN_FILE!>expose<!>() = Private()
}

// Exposes 'PrivateInFile$Private' via 'expose'
class Derived : PrivateInFile
