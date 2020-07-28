// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
// FILE: TestCase1.kt
package testPackCase1
import LibPackCase1.a.*
import LibPackCase1.b.*
fun case1 (){
    var b: B? = null
    <!DEBUG_INFO_CALL("fqName: LibPackCase1.b.plusAssign; typeCall: inline operator extension function")!>b += { C() }<!>

    <!DEBUG_INFO_CALL("fqName: LibPackCase1.a.plusAssign; typeCall: inline operator extension function")!>b += {1}<!>
}

class B {
    private inline operator fun plusAssign(c: () -> C) {}

    private inline operator fun plus(c: () -> C): C = c()
}


// FILE: LibCase1a.kt
package LibPackCase1.a
import testPackCase1.B

inline operator fun B?.plusAssign( c: ()->Int) {} //(1)


// FILE: LibCase1b.kt
package LibPackCase1.b
import LibPackCase1.a.*
import testPackCase1.B

inline operator fun B?.plusAssign( c: ()->C) {} //(2)

class C
