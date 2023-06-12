// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-448
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, operator-call -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 3 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: plusAssign as inline function
 */

// FILE: LibCase1b.kt
// TESTCASE NUMBER: 1
package LibPackCase1.b

import testPackCase1.B
import LibPackCase1.a.*

inline operator fun B?.plusAssign( c: ()->C) { //(2)
    val x = {1}
    <!DEBUG_INFO_CALL("fqName: LibPackCase1.a.plusAssign; typeCall: inline operator extension function")!>this += x<!> //to (1)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit")!>this += x<!>
}

class C

// FILE: TestCase1.kt
package testPackCase1

import LibPackCase1.a.*
import LibPackCase1.b.*

class B {
    private inline operator fun plusAssign(crossinline c: ()->C) {}
    private inline operator fun plus(crossinline c: ()->C): C =C()
}

// FILE: LibCase1a.kt
package LibPackCase1.a
import testPackCase1.B

inline operator fun B?.plusAssign( c: ()->Int) { } //(1)

