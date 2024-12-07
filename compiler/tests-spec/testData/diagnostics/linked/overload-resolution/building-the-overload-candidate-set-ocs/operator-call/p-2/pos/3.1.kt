// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -EXTENSION_SHADOWED_BY_MEMBER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, operator-call -> paragraph 2 -> sentence 3
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 1
 * overload-resolution, building-the-overload-candidate-set-ocs, operator-call -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Non-extension member callables
 */


// FILE: LibCase1.kt
// TESTCASE NUMBER: 1, 2
// TESTCASE NUMBER: 1, 2
package libPackage

import testPackCase1.Case
import testPackCase1.Case.Inv
import testPackCase1.Case.E
import kotlin.reflect.KProperty

operator fun Case.E.plus(value: Int) =  Inv()
operator fun Case.Inv.invoke(i: Int) = 1


// FILE: TestCase1.kt
// TESTCASE NUMBER: 1, 2
package testPackCase1
import libPackage.plus
import libPackage.*
import libPackage.invoke

class Case() {

    class E(val plus: Inv? = null) {
        operator fun plus(value: Int) = Case()
    }

    class Inv() {
        operator fun invoke(value: Int) = Case()
    }

    fun foo(e: E) {
        operator fun E.plus(value: Int) = Case()

        run {
            <!DEBUG_INFO_CALL("fqName: testPackCase1.Case.E.plus; typeCall: operator function")!>e+1<!>
        }
        <!DEBUG_INFO_CALL("fqName: testPackCase1.Case.E.plus; typeCall: operator function")!>e+1<!>
        e.<!DEBUG_INFO_CALL("fqName: testPackCase1.Case.E.plus; typeCall: operator function")!>plus(1)<!>
        e.plus?.<!DEBUG_INFO_CALL("fqName: testPackCase1.Case.Inv.invoke; typeCall: operator function")!>invoke(1)<!> //ok
    }
}

// FILE: LibCase3.kt
// TESTCASE NUMBER: 3, 4
package libPackage
import testPackCase3.Case
import testPackCase3.Case.Inv
import testPackCase3.Case.E

operator fun Case.E.plus(value: Int) = Inv()
operator fun Case.Inv.invoke(i: Int) = 1


// FILE: TestCase3.kt
// TESTCASE NUMBER: 3, 4
package testPackCase3
import libPackage.plus
import libPackage.*
import libPackage.invoke

class Case() {

    class E(val plus: Inv? = null) {
        operator fun plus(value: Int) = Case()
    }

    class Inv() {
        operator fun invoke(value: Int) = Case()
    }

    fun foo(e: E) {
        operator fun E.plus(value: Int) = Case()

        run {
            operator fun E.plus(value: Int) = Case()

            <!DEBUG_INFO_CALL("fqName: testPackCase3.Case.E.plus; typeCall: operator function")!>e+1<!>
        }
        <!DEBUG_INFO_CALL("fqName: testPackCase3.Case.E.plus; typeCall: operator function")!>e+1<!>
    }
    fun boo(e: E) {
        /*operator*/ fun E.plus(value: Int) = Case()

        run {
            /*operator*/ fun E.plus(value: Int) = Case()

            <!DEBUG_INFO_CALL("fqName: testPackCase3.Case.E.plus; typeCall: operator function")!>e+1<!>
        }
        <!DEBUG_INFO_CALL("fqName: testPackCase3.Case.E.plus; typeCall: operator function")!>e+1<!>
    }
}

// FILE: LibCase5.kt
// TESTCASE NUMBER: 5, 6
package libPackage

import testPackCase5.Case
import testPackCase5.Case.Inv
import testPackCase5.Case.E

operator fun Case.E.plusAssign(value: Int) {}
operator fun Case.Inv.invoke(i: Int) {}


// FILE: TestCase6.kt
// TESTCASE NUMBER: 5, 6
package testPackCase5
import libPackage.plusAssign
import libPackage.*
import libPackage.invoke

class Case() {

    class E(val plusAssign: Inv? = null) {
        operator fun plusAssign(value: Int) {}
    }

    class Inv() {
        operator fun invoke(value: Int) {}
    }

    fun foo(e: E) {
        operator fun Case.E.plusAssign(value: Int) {}

        run {
            <!DEBUG_INFO_CALL("fqName: testPackCase5.Case.E.plusAssign; typeCall: operator function")!>e+=1<!>
        }
        <!DEBUG_INFO_CALL("fqName: testPackCase5.Case.E.plusAssign; typeCall: operator function")!>e+=1<!>
        e.<!DEBUG_INFO_CALL("fqName: testPackCase5.Case.E.plusAssign; typeCall: operator function")!>plusAssign(1)<!>
        e.plusAssign?.<!DEBUG_INFO_CALL("fqName: testPackCase5.Case.Inv.invoke; typeCall: operator function")!>invoke(1)<!> //ok
    }
}


// FILE: LibCase7.kt
// TESTCASE NUMBER: 7, 8
package libPackage
import testPackCase8.Case
import testPackCase8.Case.Inv
import testPackCase8.Case.E

operator fun Case.E.plusAssign(value: Int) {}
operator fun Case.Inv.invoke(i: Int) {}

// FILE: TestCase8.kt
// TESTCASE NUMBER: 7, 8
package testPackCase8
import libPackage.plusAssign
import libPackage.*
import libPackage.invoke

class Case() {

    class E(val plusAssign: Inv? = null) {
        operator fun plusAssign(value: Int) {}
    }

    class Inv() {
        operator fun invoke(value: Int) {}
    }

    fun foo(e: E) {
        operator fun E.plusAssign(value: Int) {}

        run {
            operator fun E.plusAssign(value: Int) {}

            <!DEBUG_INFO_CALL("fqName: testPackCase8.Case.E.plusAssign; typeCall: operator function")!>e+=1<!>
        }
        <!DEBUG_INFO_CALL("fqName: testPackCase8.Case.E.plusAssign; typeCall: operator function")!>e+=1<!>
    }
    fun boo(e: E) {
        /*operator*/ fun E.plusAssign(value: Int) {}

        run {
            /*operator*/ fun E.plusAssign(value: Int) {}

            <!DEBUG_INFO_CALL("fqName: testPackCase8.Case.E.plusAssign; typeCall: operator function")!>e+=1<!>
        }
        <!DEBUG_INFO_CALL("fqName: testPackCase8.Case.E.plusAssign; typeCall: operator function")!>e+=1<!>
    }
}


// FILE: LibCase9.kt
// TESTCASE NUMBER: 9, 10
package libPackage
import testPackCase10.Iterable
import testPackCase10.Inv

operator fun Iterable.iterator() : CharIterator = TODO()
operator fun Inv.invoke() {}

// FILE: TestCase10.kt
// TESTCASE NUMBER: 9, 10
package testPackCase10
import libPackage.iterator
import libPackage.invoke

class Iterable(iterator: Inv) {
    operator fun iterator() : CharIterator = TODO()
}

class Inv(val c: Char) {
    operator fun invoke(): CharIterator = TODO()
}

operator fun Iterable.iterator() : CharIterator = TODO()

fun case(){
    operator fun Iterable.iterator() : CharIterator = TODO()
    val iterable: Iterable = Iterable(Inv('c'))

    fun foo(){
        iterable.<!DEBUG_INFO_CALL("fqName: testPackCase10.Iterable.iterator; typeCall: operator function")!>iterator()<!>
        for (i in iterable) {
            println(i)
        }
    }
    iterable.<!DEBUG_INFO_CALL("fqName: testPackCase10.Iterable.iterator; typeCall: operator function")!>iterator()<!>
    for (i in iterable) {
        println(i)
    }
}
