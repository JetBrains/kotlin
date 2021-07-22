// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


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
