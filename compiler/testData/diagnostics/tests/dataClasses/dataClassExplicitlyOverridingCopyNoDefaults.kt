// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProhibitDataClassesOverridingCopy

interface WithCopy<T> {
    fun copy(str: T): WithCopy<T>
}

<!DATA_CLASS_OVERRIDE_DEFAULT_VALUES_ERROR!>data<!> class Test(val str: String) : WithCopy<String> {
    <!CONFLICTING_OVERLOADS!>override fun copy(str: String)<!> = Test(str)
}
