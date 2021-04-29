// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT



// FILE: TestCase1.kt
/*
 * TESTCASE NUMBER: 1
 * NOTE:
 */
package testPackCase1
import LibPackCase1.a.plusAssign

fun case1 (){
    var b: B = B()
    b +={ <!ARGUMENT_TYPE_MISMATCH, TYPE_MISMATCH!>C()<!> }
    b +={ 1 }
}

class B
class C

// FILE: LibCase1b.kt
package LibPackCase1.b
import LibPackCase1.a.*
import testPackCase1.B
import testPackCase1.C

operator fun B?.plusAssign( c: ()->C) {} //(2)


// FILE: LibCase1a.kt
package LibPackCase1.a
import testPackCase1.B

operator fun B?.plusAssign( c: ()->Int) {} //(1)
