// ORIGINAL: /compiler/testData/diagnostics/tests/dataClasses/dataClassExplicitlyOverridingCopyWithDefaults.fir.kt
// WITH_STDLIB
// !LANGUAGE: +ProhibitDataClassesOverridingCopy

interface WithCopy<T> {
    fun copy(str: T): WithCopy<T>
}

data class Test(val str: String) : WithCopy<String> {
    override fun copy(str: String = this.str) = Test(str)
}

fun box() = "OK"
