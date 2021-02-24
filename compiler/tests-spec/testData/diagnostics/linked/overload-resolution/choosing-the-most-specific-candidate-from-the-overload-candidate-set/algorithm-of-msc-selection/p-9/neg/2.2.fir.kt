// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// FILE: TestCase1.kt
/*
 * TESTCASE NUMBER: 1
 * NOTE:
 */
package testPackCase1

import LibPackCase1.a.boo
import LibPackCase1.b.boo

fun case1 (b: B?){
    b.<!AMBIGUITY!>boo<!>({ C() })
    b.<!AMBIGUITY!>boo<!>({1})
}

class B {
//    private fun boo(c: () -> C) {}
}
class C

// FILE: LibCase11.kt
package LibPackCase1.b
import LibPackCase1.a.*
import testPackCase1.B
import testPackCase1.C

fun B?.boo( c: ()->C) {} //(2)


// FILE: LibCase12.kt
package LibPackCase1.a
import testPackCase1.B

fun B?.boo( c: ()->Int) { //(1)
}

// FILE: TestCase2.kt
/*
 * TESTCASE NUMBER: 2
 */
package testPackCase2

import LibPackCase2.a.*
import LibPackCase2.b.*

fun case2 (b: B?){
    b.<!AMBIGUITY!>boo<!>({ C() })
    b.<!AMBIGUITY!>boo<!>({1})
}

class B {
//    private fun boo(c: () -> C) {}
}
class C

// FILE: LibCase21.kt
package LibPackCase2.b
import LibPackCase2.a.*
import testPackCase2.B
import testPackCase2.C

fun B?.boo( c: ()->C) {} //(2)


// FILE: LibCase22.kt
package LibPackCase2.a
import testPackCase2.B

fun B?.boo( c: ()->Int) { //(1)
}
