// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-387
 * MAIN LINK: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 3 -> sentence 1
 * PRIMARY LINKS: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 3 -> sentence 4
 * overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 7 -> sentence 1
 * overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 8 -> sentence 1
 * overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 17 -> sentence 1
 * overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 17 -> sentence 3
 * NUMBER: 4
 * DESCRIPTION: for every non-default argument of the call a type constraint is built unless both are built-in integer types (Companion property-like callable)
 */

// TESTCASE NUMBER: 1
class Case1() {
    companion object {
        operator fun invoke(x: CharSequence): Unit = TODO() // (3)
        operator fun invoke(x: String, z: String = ""): String = TODO() // (4)
    }

    fun case() {
        <!DEBUG_INFO_CALL("fqName: Case1.Companion.invoke; typeCall: variable&invoke")!>Companion("")<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>Companion("")<!>
    }
}

// TESTCASE NUMBER: 2
class Case2() {
    companion object {
        operator fun invoke(y: Any?, x: Any?): Unit = TODO() // (1.1)
        operator fun invoke(vararg x: Int): String = TODO() // (1.2)
    }

    fun case() {
        <!DEBUG_INFO_CALL("fqName: Case2.Companion.invoke; typeCall: variable&invoke")!>Companion(1, 1)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>Companion(1, 1)<!>
    }
}

// TESTCASE NUMBER: 3
class Case3() {
    companion object {
        operator fun invoke(x: CharSequence, x1: String = ""): Unit = TODO() // (3)
        operator fun invoke(x: String, z: Any = ""): String = TODO() // (4)
    }

    fun case() {
        <!DEBUG_INFO_CALL("fqName: Case3.Companion.invoke; typeCall: variable&invoke")!>Companion("")<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>Companion("")<!>
    }
}

// TESTCASE NUMBER: 4
interface I {
    companion object {
        operator fun invoke(x: CharSequence): Unit = print(1) // (1)
        operator fun invoke(x: String, z: String = ""): String { print(2); return "" } // (2)
    }
}
class Case4() : I {
    companion object  {
        operator fun invoke(x: CharSequence): Unit = print(3) // (3)
        operator fun invoke(x: String, z: String = ""): Any { print(4); return "" } // (4)
    }

    fun case() {
        I.<!DEBUG_INFO_CALL("fqName: I.Companion.invoke; typeCall: operator function")!>invoke("")<!>
        I.invoke("")
        <!DEBUG_INFO_CALL("fqName: Case4.Companion.invoke; typeCall: operator function")!>invoke("")<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>invoke("")<!>
        <!DEBUG_INFO_CALL("fqName: I.Companion.invoke; typeCall: variable&invoke")!>I("")<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>I("")<!>
        <!DEBUG_INFO_CALL("fqName: Case4.Companion.invoke; typeCall: variable&invoke")!>Case4("")<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>Case4("")<!>
    }
}
