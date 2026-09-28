// ISSUE: KT-88589
// LANGUAGE: +FullValueClasses, +AllowSmartCastsOnValueClassUnderlyingProperties
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS

// MODULE: lib
// FILE: lib.kt
package lib

value class MultiField(val first: String?, val second: Int)

OPTIONAL_JVM_INLINE_ANNOTATION
value class SingleField(val only: String?)

// MODULE: main(lib)
// FILE: main.kt
import lib.MultiField
import lib.SingleField

fun multiField(v: MultiField): String = if (v.first != null) v.first else "fail: first"

fun singleField(v: SingleField): String = if (v.only != null) v.only else "fail: only"

fun box(): String {
    val result = multiField(MultiField("O", 1)) + singleField(SingleField("K"))
    return if (result == "OK") "OK" else result
}
