/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: objects, inheritance
 * NUMBER: 5
 * DESCRIPTION: Access to class members in the super constructor call of an object.
 * ISSUES: KT-25289
 */

// TESTCASE NUMBER: 5
open class Foo(val prop: Int) {
    companion object : Foo(this.prop)
}

fun box(): String? {
    if (Foo(42) == null) return null

    return "OK"
}
