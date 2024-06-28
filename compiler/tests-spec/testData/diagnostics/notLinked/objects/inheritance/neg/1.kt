// FIR_IDENTICAL

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: objects, inheritance
 * NUMBER: 1
 * DESCRIPTION: Access to class members in the super constructor call of an object.
 * ISSUES: KT-25289
 */

// TESTCASE NUMBER: 1
open class Foo(val prop: Int) {
    companion object : Foo(<!SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR!>prop<!>)
}

fun box(): String? {
    if (<!SENSELESS_COMPARISON!>Foo(42) == null<!>) return null

    return "OK"
}
