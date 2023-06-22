// !DIAGNOSTICS: -UNUSED_PARAMETER -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * MAIN LINK: type-system, introduction-1 -> paragraph 6 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: The use of Boolean literals as the identifier (with backtick) in the class.
 */

// TESTCASE NUMBER: 1
fun case_1(x: Int = <!NULL_FOR_NONNULL_TYPE!>null<!>) {
    println(x)
}

// TESTCASE NUMBER: 2
fun case_2(x: Any = <!NULL_FOR_NONNULL_TYPE!>null<!>) {
    println(x)
}

// TESTCASE NUMBER: 3
fun case_3(x: Nothing = <!NULL_FOR_NONNULL_TYPE!>null<!>) {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(x)
}

// TESTCASE NUMBER: 4
class Case4(x: Int = <!NULL_FOR_NONNULL_TYPE!>null<!>)

// TESTCASE NUMBER: 5
class Case5 constructor(x: Any = <!NULL_FOR_NONNULL_TYPE!>null<!>)

// TESTCASE NUMBER: 6
class Case6 {
    fun foo(x: Nothing = <!NULL_FOR_NONNULL_TYPE!>null<!>) {}
}

// TESTCASE NUMBER: 7
class Case7 {
    val x: Int get() = <!NULL_FOR_NONNULL_TYPE!>null<!>
}

// TESTCASE NUMBER: 8
class Case8 {
    var x: Any = 0
        get() = 0
        set(value) {
            field = <!NULL_FOR_NONNULL_TYPE!>null<!>
        }
}

// TESTCASE NUMBER: 9
fun case_9(): Any = <!NULL_FOR_NONNULL_TYPE!>null<!>

// TESTCASE NUMBER: 10
fun case_10(x: Int, y: Boolean): Any = <!RETURN_TYPE_MISMATCH!>if (y) x else null<!>

// TESTCASE NUMBER: 11
fun case_11(x: Int, y: Boolean): Any = <!RETURN_TYPE_MISMATCH!>if (y) x else null<!>

// TESTCASE NUMBER: 12
class Case12 {
    val x: Any
    var y: Any
    var z: Any
    init {
        x = <!NULL_FOR_NONNULL_TYPE!>null<!>
        y = <!NULL_FOR_NONNULL_TYPE!>null<!>
        z = 10
        z = <!NULL_FOR_NONNULL_TYPE!>null<!>
    }
}

// TESTCASE NUMBER: 13
open class Case13_1 {
    open val x: Int = 10
}

class Case13: Case13_1() {
    override val <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>x<!> = null
}

// TESTCASE NUMBER: 14
abstract class Case14_1 {
    abstract val x: Int
}

class Case14: Case14_1() {
    override val <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>x<!> = null
}

// TESTCASE NUMBER: 15
interface Case15_1 {
    fun foo(): Int = 10
}

class Case15(): Case15_1 {
    override fun <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>foo<!>() = null
}
