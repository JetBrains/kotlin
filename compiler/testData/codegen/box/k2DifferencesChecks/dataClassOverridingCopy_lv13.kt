// ORIGINAL: /compiler/testData/diagnostics/tests/dataClasses/dataClassOverridingCopy_lv13.fir.kt
// WITH_STDLIB
// !LANGUAGE: +ProhibitDataClassesOverridingCopy

interface WithCopy<T> {
    fun copy(str: T): WithCopy<T>
}

data class Test(val str: String): WithCopy<String>

fun box() = "OK"
