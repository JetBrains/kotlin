// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProhibitDataClassesOverridingCopy

interface WithCopy<T> {
    fun copy(str: T): WithCopy<T>
}

<!DATA_CLASS_OVERRIDE_DEFAULT_VALUES!>data<!> class <!CONFLICTING_OVERLOADS!>Test(val str: String)<!> : WithCopy<String> {
    override <!CONFLICTING_OVERLOADS!>fun copy(str: String = <!DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE!>this.str<!>)<!> = Test(str)
}
