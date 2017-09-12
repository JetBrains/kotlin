// !LANGUAGE: -ProhibitDataClassesOverridingCopy

interface WithCopy<T> {
    fun copy(str: T): WithCopy<T>
}

<!DATA_CLASS_OVERRIDE_DEFAULT_VALUES_WARNING!>data<!> class Test(val str: String): WithCopy<String>