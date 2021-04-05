class Public {
    private open class NestedPrivate

    fun <!EXPOSED_FUNCTION_RETURN_TYPE!>test1<!>() = NestedPrivate()
    fun test2(<!EXPOSED_PARAMETER_TYPE!>p: NestedPrivate<!>) {}
    fun <!EXPOSED_RECEIVER_TYPE!>NestedPrivate<!>.test3() {}
    val <!EXPOSED_PROPERTY_TYPE!>test4<!> = NestedPrivate()
    class Test5 : <!EXPOSED_SUPER_CLASS!>NestedPrivate<!>()
}

private class PrivateInFileClass {
    private open class NestedPrivate

    fun test1() = NestedPrivate()
    fun test2(p: NestedPrivate) {}
    fun NestedPrivate.test3() {}
    val test4 = NestedPrivate()
    class Test5 : NestedPrivate()
}

private interface PrivateInFile {
    private class Private

    fun expose() = Private()
}

// Exposes 'PrivateInFile$Private' via 'expose'
class Derived : PrivateInFile
