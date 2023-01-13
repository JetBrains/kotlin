// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// !LANGUAGE: +ProhibitDataClassesOverridingCopy

interface WithCopy<T> {
    fun copy(str: T): WithCopy<T>
}

data class Test(val str: String): WithCopy<String>