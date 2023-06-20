// ORIGINAL: /compiler/tests-spec/testData/diagnostics/notLinked/objects/inheritance/neg/3.fir.kt
// WITH_STDLIB
/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: objects, inheritance
 * NUMBER: 3
 * DESCRIPTION: Access to class members in the super constructor call of an object.
 * ISSUES: KT-25289
 */

// TESTCASE NUMBER: 3
open class Foo(val prop: Int) {
    object MyObject : Foo(MyObject.prop)
}

fun vox(): String? {
    if (Foo.MyObject == null) return null

    return "OK"
}


fun box() = vox() ?: "FAIL"
