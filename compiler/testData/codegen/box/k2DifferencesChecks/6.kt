// ORIGINAL: /compiler/tests-spec/testData/diagnostics/notLinked/objects/inheritance/neg/6.fir.kt
// WITH_STDLIB
/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: objects, inheritance
 * NUMBER: 6
 * DESCRIPTION: Access to class members in the super constructor call of an object.
 * ISSUES: KT-25289
 */

// TESTCASE NUMBER: 6
open class Foo(val prop: Int) {
    companion object : Foo(Companion.prop)
}

fun vox(): String? {
    if (Foo(42) == null) return null

    return "OK"
}


fun box() = vox() ?: "FAIL"
