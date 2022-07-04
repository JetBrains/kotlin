// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE:+AssignOperatorOverloadForJvm

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

/**
 * Test that if return type is not Unit then method has an error
 */
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun String.assign(a: String): String {
    return ""
}

class ByDelegate {
    val v: Int by Delegate()
}

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return 5
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
    }
}

data class NullCheck(val x: NullCheckContainer)
data class NullCheckContainer(var value: String)
operator fun NullCheckContainer.assign(value: String) {
}

fun test() {
    // Test a val without any assign operator still works
    val a = A()
    <!VAL_REASSIGNMENT!>a<!> = <!TYPE_MISMATCH!>"5"<!>

    // Test assign operator diagnostics
    val b = B()
    <!VAL_REASSIGNMENT!>b<!> = <!TYPE_MISMATCH!>A()<!>
    val c = C()
    <!VAL_REASSIGNMENT!>c<!> = <!TYPE_MISMATCH!>A()<!>
    c = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1L<!>

    // Test delegate
    val delegate: ByDelegate = ByDelegate()
    <!VAL_REASSIGNMENT!>delegate.v<!> = <!TYPE_MISMATCH!>"OK"<!>

    // Test "operator fun assign" return type diagnostics
    val x = ""
    <!VAL_REASSIGNMENT!>x<!> <!ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT!>=<!> ""

    // Test unsafe call diagnostics
    val nullCheck: NullCheck? = null
    nullCheck<!UNSAFE_CALL!>.<!>x = "Fail"
    <!VAL_REASSIGNMENT!>nullCheck?.x<!> = <!TYPE_MISMATCH!>"Fail"<!>
}

