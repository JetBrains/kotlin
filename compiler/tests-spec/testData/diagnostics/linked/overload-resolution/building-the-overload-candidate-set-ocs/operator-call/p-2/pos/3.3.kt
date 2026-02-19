// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -EXTENSION_SHADOWED_BY_MEMBER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-268
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, operator-call -> paragraph 2 -> sentence 3
 * PRIMARY LINKS: overload-resolution, building-the-overload-candidate-set-ocs, call-with-an-explicit-receiver -> paragraph 6 -> sentence 3
 * overload-resolution, building-the-overload-candidate-set-ocs, operator-call -> paragraph 4 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION: Explicitly imported extension callables
 */


// FILE: LibCase11.kt
// TESTCASE NUMBER: 1
package libPackage1
import testPackCase1.Case
import testPackCase1.Case.Inv
import testPackCase1.Case.E

operator fun Case.E.plus(value: Int) =  Inv()
operator fun Case.Inv.invoke(i: Int) = 1

// FILE: LibCase12.kt
// TESTCASE NUMBER: 1
package libPackage2
import testPackCase1.Case
import testPackCase1.Case.Inv
import testPackCase1.Case.E

operator fun Case.E.plus(value: Int) =  Inv()
operator fun Case.Inv.invoke(i: Int) = 1


// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testPackCase1
import libPackage1.plus
import libPackage1.invoke
import libPackage2.*
class Case() {

    class E(val plus: Inv? = null) {
        /*operator*/ fun plus(value: Int) = Case()
    }

    class Inv() {
        /*operator*/ fun invoke(value: Int) = Case()
    }

    fun foo(e: E) {
        /*operator*/ fun E.plus(value: Int) = Case()

        run {
            <!DEBUG_INFO_CALL("fqName: libPackage1.plus; typeCall: operator extension function")!>e + 1<!>
        }
        <!DEBUG_INFO_CALL("fqName: libPackage1.plus; typeCall: operator extension function")!>e + 1<!>

    }
}

// FILE: LibCase21.kt
// TESTCASE NUMBER: 2
package libPackage1
import testPackCase2.Case
import testPackCase2.Case.Inv
import testPackCase2.Case.E

operator fun Case.E.plus(value: Int) =  Inv()
operator fun Case.Inv.invoke(i: Int) = 1

// FILE: LibCase22.kt
// TESTCASE NUMBER: 2
package libPackage2
import testPackCase2.Case
import testPackCase2.Case.Inv
import testPackCase2.Case.E

operator fun Case.E.plus(value: Int) =  Inv()
operator fun Case.Inv.invoke(i: Int) = 1


// FILE: TestCase2.kt
// TESTCASE NUMBER: 2
package testPackCase2
import libPackage1.plus
import libPackage1.invoke
import libPackage2.*
class Case() {

    class E(val plus: Inv? = null) {
        /*operator*/ fun plus(value: Int) = Case()
    }

    class Inv() {
        /*operator*/ fun invoke(value: Int) = Case()
    }

    fun foo(e: E) {
        /*operator*/ fun E.plus(value: Int) = Case()

        run {
            /*operator*/ fun E.plus(value: Int) = Case()

            <!DEBUG_INFO_CALL("fqName: libPackage1.plus; typeCall: operator extension function")!>e + 1<!>
        }
        <!DEBUG_INFO_CALL("fqName: libPackage1.plus; typeCall: operator extension function")!>e + 1<!>
    }
}

// FILE: LibCase3.kt
// TESTCASE NUMBER: 3
package libPackage
import testPackCase3.Case
import testPackCase3.Case.Inv
import testPackCase3.Case.E

operator fun Case.E.plusAssign(value: Int) {}
operator fun Case.Inv.invoke(i: Int) {}


// FILE: TestCase3.kt
// TESTCASE NUMBER:  3
package testPackCase3
import libPackage.plusAssign
import libPackage.invoke
import libPackage.*
class Case() {

    class E(val plusAssign: Inv? = null) {
        /*operator*/ fun plusAssign(value: Int) {}
    }

    class Inv() {
        /*operator*/ fun invoke(value: Int) {}
    }

    fun foo(e: E) {
        /*operator*/ fun E.plusAssign(value: Int) {}

        run {
            <!DEBUG_INFO_CALL("fqName: libPackage.plusAssign; typeCall: operator extension function")!>e += 1<!>
        }
        <!DEBUG_INFO_CALL("fqName: libPackage.plusAssign; typeCall: operator extension function")!>e += 1<!>

    }
}


// FILE: LibCase4.kt
// TESTCASE NUMBER: 4
package libPackage
import testPackCase4.Case
import testPackCase4.Case.Inv
import testPackCase4.Case.E

operator fun Case.E.plusAssign(value: Int) {}
operator fun Case.Inv.invoke(i: Int) {}


// FILE: TestCase4.kt
// TESTCASE NUMBER: 4
package testPackCase4
import libPackage.plusAssign
import libPackage.*
import libPackage.invoke

operator fun Case.E.plusAssign(value: Int) {}

class Case() {

    class E(val plusAssign: Inv? = null) {
        /*operator*/ fun plusAssign(value: Int) {}
    }

    class Inv() {
        /*operator*/ fun invoke(value: Int) {}
    }

    fun foo(e: E) {
        /*operator*/ fun E.plusAssign(value: Int) {}

        run {
            /*operator*/ fun E.plusAssign(value: Int) {}

            <!DEBUG_INFO_CALL("fqName: libPackage.plusAssign; typeCall: operator extension function")!>e += 1<!>
        }
        <!DEBUG_INFO_CALL("fqName: libPackage.plusAssign; typeCall: operator extension function")!>e += 1<!>
    }
}
