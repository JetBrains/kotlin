// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// FILE: TestCase1.kt
/*
 * TESTCASE NUMBER: 1
 */
package testPackCase1

class B {
    inline operator fun plusAssign(crossinline c: ()->C) {}

    inline operator fun plus(crossinline c: ()->C): C = C()
}

@JvmName("bb")
inline operator fun B?.plusAssign( c: ()->Any) { } //(1)
@JvmName("aa")
inline  operator fun B?.plusAssign( c: ()->C) { //(2)

    this += {1}
    <!DEBUG_INFO_CALL("fqName: testPackCase1.plusAssign; typeCall: inline operator extension function")!>this += {1}<!>
}

class C
