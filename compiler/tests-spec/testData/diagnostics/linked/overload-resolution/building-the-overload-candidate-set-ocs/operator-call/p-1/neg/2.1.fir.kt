// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// FILE: TestCase1.kt
/*
 * TESTCASE NUMBER: 1
 */
package testPackCase1

class C
@JvmName("bb1")
fun C?.boo( c: ()->Any) {} //(1)

@JvmName("aa1")
fun C?.boo( c: ()->C) : String { //(2)
    val x = {1}
    this.<!DEBUG_INFO_CALL("fqName: testPackCase1.boo; typeCall: extension function")!>boo( x )<!>// ok to (1)
    this.boo( x )

    this.<!DEBUG_INFO_CALL("fqName: testPackCase1.boo; typeCall: extension function")!>boo( {1})<!> //to (2); {1} is ()->C
    this.boo( {1})

    return ""
}
// FILE: TestCase2.kt
/*
 * TESTCASE NUMBER: 2
 */
package testPackCase2

class C
@JvmName("bb1")
fun C?.boo( c: ()->Any) {} //(1)

@JvmName("aa1")
fun C?.boo( c: ()->C, x : Int = 1) : String { //(2)
    val x = {1}
    this.<!DEBUG_INFO_CALL("fqName: testPackCase2.boo; typeCall: extension function")!>boo( x )<!>// ok to (1)
    this.boo( x )

    this.<!DEBUG_INFO_CALL("fqName: testPackCase2.boo; typeCall: extension function")!>boo( {1})<!> //to (2); {1} is ()->C
    this.boo( {1})

    return ""
}
