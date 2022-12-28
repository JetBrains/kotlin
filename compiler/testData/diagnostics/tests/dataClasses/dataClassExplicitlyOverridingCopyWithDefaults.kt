// !LANGUAGE: +ProhibitDataClassesOverridingCopy

interface WithCopy<T> {
    fun copy(str: T): WithCopy<T>
}

<!DATA_CLASS_OVERRIDE_DEFAULT_VALUES_ERROR!>data<!> class <!CONFLICTING_JVM_DECLARATIONS, CONFLICTING_JVM_DECLARATIONS, CONFLICTING_JVM_DECLARATIONS!>Test(val str: String)<!> : WithCopy<String> {
    <!CONFLICTING_OVERLOADS!>override fun copy(str: String = <!DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE!>this.str<!>)<!> = Test(str)
}