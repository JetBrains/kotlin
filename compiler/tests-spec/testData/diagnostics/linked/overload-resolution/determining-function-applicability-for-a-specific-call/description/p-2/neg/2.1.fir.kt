// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// FILE: TestCase1.kt
/*
 * TESTCASE NUMBER: 1
 */
package testPackCase1

import LibPackCase1.a.boo
import LibPackCase1.b.*

fun case1 (b: B?){
    b.boo({ C() })
    b.boo({1})
}

class B {
    private fun boo(c: () -> C) {}
}
class C

// FILE: LibCase1.kt
package LibPackCase1.b
import LibPackCase1.a.*
import testPackCase1.B
import testPackCase1.C

fun B?.boo( c: ()->C) {} //(2)


// FILE: LibCase1.kt
package LibPackCase1.a
import testPackCase1.B

fun B?.boo( c: ()->Int) { //(1)
}
