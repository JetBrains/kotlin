// !DIAGNOSTICS: -UNNECESSARY_SAFE_CALL -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// FILE: TestCase1.kt
// TESTCASE NUMBER: 1
package testPackCase1

fun case1(a: A, c: C) {
    a?.b <!UNSAFE_CALL!>.<!><!DEBUG_INFO_CALL("fqName: fqName is unknown; typeCall: unresolved")!>plusAssign(c)<!>

    val x = {
        a?.b<!UNSAFE_CALL!>.<!><!DEBUG_INFO_CALL("fqName: fqName is unknown; typeCall: unresolved")!>plusAssign(c)<!>
    }()

    a?.b<!UNSAFE_CALL!>.<!><!DEBUG_INFO_CALL("fqName: fqName is unknown; typeCall: unresolved")!>plusAssign({ c }())<!>
}

class A(val b: B)

class B {
    operator fun plusAssign(c: C) {
        print("1")
    }

    operator fun plus(c: C): C {
        print("2")
        return c
    }
}

class C

// FILE: TestCase2.kt
// TESTCASE NUMBER: 2
package testPackCase2

fun case2(a: A?, c: C) {

    <!DEBUG_INFO_CALL("fqName: testPackCase2.B.plusAssign; typeCall: operator function")!>a?.b += c<!>
    a?.b.<!DEBUG_INFO_CALL("fqName: testPackCase2.plusAssign; typeCall: operator extension function")!>plusAssign(c)<!>

    val x = {
        <!DEBUG_INFO_CALL("fqName: testPackCase2.B.plusAssign; typeCall: operator function")!>a?.b += c<!>
        a?.b.<!DEBUG_INFO_CALL("fqName: testPackCase2.plusAssign; typeCall: operator extension function")!>plusAssign(c)<!>
    }()

    <!DEBUG_INFO_CALL("fqName: testPackCase2.B.plusAssign; typeCall: operator function")!>a?.b += { c }()<!>

    a?.b.<!DEBUG_INFO_CALL("fqName: testPackCase2.plusAssign; typeCall: operator extension function")!>plusAssign({ c }())<!>

}

class A(val b: B)

class B {
    operator fun plusAssign(c: C) {
        print("1")
    }

    operator fun plus(c: C): C {
        print("2")
        return c
    }
}

operator fun B?.plusAssign(c: C) {
    print("3")
}

class C
// FILE: TestCase3.kt
// TESTCASE NUMBER: 3
package testPackCase3

fun case3(a: A?, c: C) {

    <!DEBUG_INFO_CALL("fqName: testPackCase3.B.plusAssign; typeCall: operator function")!>a?.b += c<!>
    a?.b.<!DEBUG_INFO_CALL("fqName: testPackCase3.plusAssign; typeCall: operator extension function")!>plusAssign(c)<!>

    val x = {
        <!DEBUG_INFO_CALL("fqName: testPackCase3.B.plusAssign; typeCall: operator function")!>a?.b += c<!>
        a?.b.<!DEBUG_INFO_CALL("fqName: testPackCase3.plusAssign; typeCall: operator extension function")!>plusAssign(c)<!>
    }()

    <!DEBUG_INFO_CALL("fqName: testPackCase3.B.plusAssign; typeCall: operator function")!>a?.b += { c }()<!>

    a?.b.<!DEBUG_INFO_CALL("fqName: testPackCase3.plusAssign; typeCall: operator extension function")!>plusAssign({ c }())<!>

}

class A(val b: B)

class B {
    operator fun plusAssign(c: C) {
        print("1")
    }

    operator fun plus(c: C): C {
        print("2")
        return c
    }
}

operator fun B?.plusAssign(c: C) {
    print("3")
}

operator fun B?.plusAssign(c: Any) {
    print("4")
}


class C
