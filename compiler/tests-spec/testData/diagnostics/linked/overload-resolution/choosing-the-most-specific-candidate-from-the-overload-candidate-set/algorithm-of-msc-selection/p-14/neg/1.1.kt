// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-387
 * MAIN LINK: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 14 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: an overload ambiguity which must be reported if several candidates which are equally applicable for the call exests
 */
// TESTCASE NUMBER: 1
class Case1() {
    fun foo(x: Int, y: Number?): Unit = TODO() // (1)
    fun foo(vararg x: Short): Unit = TODO()//(2)

    fun testcase() {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(1, 1)
    }
}

// TESTCASE NUMBER: 2
class Case2() {
    fun foo(y: Any, x: Any): Unit = TODO() // (1.1)
    fun foo(vararg x: Int?): Unit = TODO() // (1.2)

    fun case(){
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(1, 1)
    }
}
// TESTCASE NUMBER: 3
class Case3() {
    fun foo(y: Any, x: Any): Unit = TODO() // (1.1)
    fun foo(vararg x: Int?): Unit = TODO() // (1.2)

    fun case(){
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(1, 1)
    }
}

// TESTCASE NUMBER: 4
class Case4() {
    fun foo(y: Int, x: Short): Unit = TODO() // (1.1)
    fun foo(y: Int, x: Long): Unit = TODO() // (1.2)

    fun case(){
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(1, 1)
    }
}


// TESTCASE NUMBER: 5
class Case5() {
    fun foo(y: Int, x: Short): Unit = TODO() // (1.1)
    fun foo(y: Int, x: Long, a : Any =""): Unit = TODO() // (1.2)

    fun case(){
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(1, 1)
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(1, 1)
    }
}

// TESTCASE NUMBER: 6

class A : B, C
interface B
interface C
fun foo(x: B) {} //(1)
fun foo(y: C, z: String = "foo") {} //2
fun bar() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(A()) //OVERLOAD_RESOLUTION_AMBIGUITY
}
