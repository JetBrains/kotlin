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
import LibPackCase1.b.plusAssign

fun case1 (){
    var b: B? = B()
    <!AMBIGUITY!>b += { C() }<!>

    <!AMBIGUITY!>b += {1}<!>
}

class B {
    private  operator fun plusAssign(c: () -> C) {}
//    private  operator fun plus(c: () -> C): C = c()
}
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
