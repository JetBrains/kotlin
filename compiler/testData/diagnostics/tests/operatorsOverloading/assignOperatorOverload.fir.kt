// !DIAGNOSTICS: -UNUSED_PARAMETER
// !RENDER_DIAGNOSTICS_FULL_TEXT
// !LANGUAGE:+AssignOperatorOverload

import kotlin.reflect.KProperty

class A
class B
operator fun B.assign(a: String) {
}

class C {
    fun assign(a: A) {
    }
}

operator fun C.assign(a: String) {
}

operator fun C.assign(a: Int) {
}

operator fun Int.assign(a: String) {
}

operator fun DContainer.assign(other: Int) {
}
operator fun D.get(i: Int): DContainer {
    return this.x
}
data class D(val x: DContainer = DContainer())
class DContainer

/**
 * Test that if return type is not Unit then method has an error
 */
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun String.assign(a: String): String {
    return ""
}

data class NullCheck(val x: NullCheckContainer)
data class NullCheckContainer(var value: String)
operator fun NullCheckContainer.assign(value: String) {
}

fun test() {
    // Test a val without any assign operator still works
    val a = A()
    <!VAL_REASSIGNMENT!>a<!> = <!ASSIGNMENT_TYPE_MISMATCH!>"5"<!>

    // Test assign operator diagnostics
    val b = B()
    <!VAL_REASSIGNMENT!>b<!> = <!ASSIGNMENT_TYPE_MISMATCH!>A()<!>
    val c = C()
    <!VAL_REASSIGNMENT!>c<!> = <!ASSIGNMENT_TYPE_MISMATCH!>A()<!>
    <!VAL_REASSIGNMENT!>c<!> = <!ASSIGNMENT_TYPE_MISMATCH!>1L<!>

    // Test that d.get(0).assign(42) is not possible with operators and thus not ambigious with d.set(0, 42)
    val d = D()
    val dContainer = d[0]
    dContainer = 42
    d<!NO_SET_METHOD!>[0]<!> = 42
    d.<!FUNCTION_CALL_EXPECTED!>get<!>(0) = <!ASSIGNMENT_TYPE_MISMATCH!>42<!>

    // Test "operator fun assign" return type diagnostics
    val x = ""
    x <!ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT!>=<!> ""

    // Test unsafe call diagnostics
    val nullCheck: NullCheck? = null
    nullCheck<!UNSAFE_CALL!>.<!>x = <!ASSIGNMENT_TYPE_MISMATCH!>"Fail"<!>
    nullCheck?.x = "Fail"
}

