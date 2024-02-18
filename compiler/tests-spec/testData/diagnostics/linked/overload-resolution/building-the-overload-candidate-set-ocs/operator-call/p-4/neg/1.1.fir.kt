// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-268
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, operator-call -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: properties available through the invoke convention are non-eligible for operator calls
 */

// FILE: TestCase1.kt
/*
 * TESTCASE NUMBER: 1
 * NOTE:Delegated properties
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-36844
 */
package testPackCase1

fun case1() {
    var b = B()
    val a: String = b.p
}

class B() {
    val p: String by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE, PROPERTY_AS_OPERATOR!>Delegate()<!> // DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE expected
}

class Delegate {
    val getValue = G()
}

class G {
    operator fun invoke(i: Int): String = ""
}


// FILE: TestCase2.kt
/*
 * TESTCASE NUMBER: 2
 * NOTE:Delegated properties
 */
package testPackCase2
fun case2() {
    var b = B()
    val a: String = b.p
    b.p = ""
}

class B() {
    var p: String by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE, DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE, PROPERTY_AS_OPERATOR, PROPERTY_AS_OPERATOR!>Delegate()<!> // DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE expected
}

class Delegate {
    val getValue = G()
    val setValue = S()
}

class G {
    operator fun invoke(i: Int): String = ""
}

class S {
    operator fun invoke(i: String) {}
}

// FILE: TestCase3.kt
/*
 * TESTCASE NUMBER: 3
 * NOTE: Arithmetic and comparison operators
 */
package testPackCase3

class Comp() {
    operator fun invoke(i: Int): Int = 1
}

class Arifm() {
    operator fun invoke(i: Int) {}
}

class Contains() {
    operator fun invoke(i: Int): Boolean = true
}

class Getter() {
    operator fun invoke(i: Int): Int = 1
}

class Setter() {
    operator fun invoke(i: Int, j: Int) {}
}

class Unary<B>() {
    operator fun invoke(): B = TODO()
}

class B() {
    val plus = Arifm()
    val minus = Arifm()
    val compareTo = Comp()
    val contains = Contains()
    val get = Getter()
    val set = Setter()
    val unaryPlus = Unary<B>()
}

fun case3() {
    var b = B()
    b <!PROPERTY_AS_OPERATOR!>+<!> 5
    b <!PROPERTY_AS_OPERATOR!>-<!> 5
    b <!PROPERTY_AS_OPERATOR!><<!> 5
    b <!PROPERTY_AS_OPERATOR!>>=<!> 5
    1 <!PROPERTY_AS_OPERATOR!>in<!> b
    <!PROPERTY_AS_OPERATOR!>b[2]<!>
    <!PROPERTY_AS_OPERATOR!>b[3]<!> = 4
    <!PROPERTY_AS_OPERATOR!>+<!>b
}

// FILE: TestCase4.kt
/*
 * TESTCASE NUMBER: 4
 * NOTE: Operator-form assignments
 */
package testPackCase4
class Assign() {
    operator fun invoke(i: Int) {}
}

class B(val minusAssign: Assign = Assign()) {
    val plusAssign = Assign()
}

fun case3() {
    var b = B()
    b  <!PROPERTY_AS_OPERATOR!>+=<!>  2
    b  <!PROPERTY_AS_OPERATOR!>-=<!>  3
}

// FILE: TestCase5.kt
/*
 * TESTCASE NUMBER: 5
 * NOTE: Arithmetic and comparison operators
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-36852
 */
package testPackCase5

class Case5() {

    fun f(c: Case5){
        this <!OPERATOR_CALL_ON_CONSTRUCTOR!>+<!> 1 // OPERATOR_CALL_ON_CONSTRUCTOR/OPERATOR_MODIFIER_REQUIRED for class plus, resolved to (1)
        c <!OPERATOR_CALL_ON_CONSTRUCTOR!>+<!> 1 // OPERATOR_CALL_ON_CONSTRUCTOR/OPERATOR_MODIFIER_REQUIRED for class plus, resolved to (1)
    }

    inner class plus /* (1) */ constructor(val i:Int){
        operator fun invoke(i:Int) {}
    }
}

// FILE: TestCase6.kt
/*
 * TESTCASE NUMBER: 6
 * NOTE: Arithmetic and comparison operators
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-36852
 */
package testPackCase6

class B(var a: Int = 0) {

    inner  class E(){
        val plus :E =TODO()

        fun foo(b: B){
            this <!PROPERTY_AS_OPERATOR!>+<!> 1
        }

        operator fun invoke(value: Int) = B()
    }
}

// FILE: TestCase7.kt
/*
 * TESTCASE NUMBER: 7
 * NOTE: for-loop operators
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-36898, KT-62356
 */
package testPackCase7

fun case7 () {
    val iterable: Iterable = Iterable(Inv('s'))
    for (i in <!PROPERTY_AS_OPERATOR!>iterable<!>) {
        println(i)
    }
}

class Iterable(val iterator: Inv) {
    //  operator fun iterator() : CharIterator = TODO()
}

class Inv(val c: Char) {
    operator fun invoke(): CharIterator = object : CharIterator() {
        private var index = 0

        override fun nextChar(): Char {
            index++; return c
        }

        override fun hasNext(): Boolean = index < 5
    }
}


// FILE: TestCase8.kt
/*
 * TESTCASE NUMBER: 8
 * NOTE: for-loop operators
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-36898, KT-62356
 */
package testPackCase8

fun case8 () {
    val iterable: Iterable = Iterable()
    for (i in <!PROPERTY_AS_OPERATOR!>iterable<!>) {
        println(i)
    }
}

class Iterable() {
    //  operator fun iterator() : CharIterator = TODO()
}
val Iterable.iterator: Inv
    get() = Inv('c')

class Inv(val c: Char) {
    operator fun invoke(): CharIterator = object : CharIterator() {
        private var index = 0

        override fun nextChar(): Char {
            index++; return c
        }

        override fun hasNext(): Boolean = index < 5
    }
}
