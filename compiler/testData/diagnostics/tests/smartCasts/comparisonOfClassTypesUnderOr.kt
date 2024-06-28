// ISSUE: KT-1982
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST

interface A
class B : A

fun test1(x:Any?) {
    if (x is A || x !is B) {
        var k: B? = <!TYPE_MISMATCH!>x<!>
        var k2: A? = <!TYPE_MISMATCH!>x<!>
        var k3: Any? = x
    }
    else {
        var k: Any = x
        var k2: A = x
        var k3: B = x
    }
}

fun test2(x:Any?) {
    if (x is A || x is B) {
        var k: B = <!TYPE_MISMATCH!>x<!>
        var k2: A = <!TYPE_MISMATCH!>x<!>
        var k3: Any = x
    }
    else {
        var k: Any? = x
        var k2: A = <!TYPE_MISMATCH!>x<!>
        var k3: B = <!TYPE_MISMATCH!>x<!>
    }
}

fun test3(x:Any?) {
    if (x !is A || x is B) {
        var k: B = <!TYPE_MISMATCH!>x<!>
        var k2: A = <!TYPE_MISMATCH!>x<!>
        var k3: Any? = x
    }
    else {
        var k: Any = x
        var k2: A = x
        var k3: B = <!TYPE_MISMATCH!>x<!>
    }
}

fun test4(x:Any?) {
    if (x is A || x is B || x == null) {
        var k: B? = <!TYPE_MISMATCH!>x<!>
        var k2: A? = <!TYPE_MISMATCH!>x<!>
        var k3: Any? = x
    }
    else {
        var k: Any = x
        var k2: A = <!TYPE_MISMATCH!>x<!>
        var k3: B = <!TYPE_MISMATCH!>x<!>
    }
}

sealed interface A2
class B2 : A2
class C2 : A2

fun test5(x:Any?) {
    if (x is C2 || x is B2) {
        var k: B2 = <!TYPE_MISMATCH!>x<!>
        var k2: A2 = <!TYPE_MISMATCH!>x<!>
        var k3: C2 = <!TYPE_MISMATCH!>x<!>
        var k4: Any = x
    }
}

fun test6(x:Any?) {
    if (x is C2 || x !is B2) {
        var k: B2 = <!TYPE_MISMATCH!>x<!>
        var k2: A2 = <!TYPE_MISMATCH!>x<!>
        var k3: C2 = <!TYPE_MISMATCH!>x<!>
        var k4: Any? = x
    } else {
        var k: Any = x
        var k2: A2 = x
        var k3: B2 = x
    }
}

fun test7(x:Any?) {
    if (x is A2 || x !is B2) {
        var k: B2 = <!TYPE_MISMATCH!>x<!>
        var k2: A2 = <!TYPE_MISMATCH!>x<!>
        var k3: C2 = <!TYPE_MISMATCH!>x<!>
        var k4: Any? = x
    }
    else {
        var k: Any = x
        var k2: A2 = x
        var k3: B2 = x
        var k4 : C2 = <!TYPE_MISMATCH!>x<!>
    }
}

fun test8(x:Any?) {
    if (x is A2 || x is B2) {
        var k: B2 = <!TYPE_MISMATCH!>x<!>
        var k2: A2 = <!TYPE_MISMATCH!>x<!>
        var k3: C2 = <!TYPE_MISMATCH!>x<!>
        var k4: Any? = x
    }
    else {
        var k: Any? = x
        var k2: A2 = <!TYPE_MISMATCH!>x<!>
        var k3: B2 = <!TYPE_MISMATCH!>x<!>
        var k4 : C2 = <!TYPE_MISMATCH!>x<!>
    }
}

fun test9(x:Any?) {
    if (x !is A2 || x is B2) {
        var k: B2 = <!TYPE_MISMATCH!>x<!>
        var k2: A2 = <!TYPE_MISMATCH!>x<!>
        var k3: C2 = <!TYPE_MISMATCH!>x<!>
        var k4: Any? = x
    }
    else {
        var k: Any = x
        var k2: A2 = x
        var k3: B2 = <!TYPE_MISMATCH!>x<!>
        var k4 : C2 = <!TYPE_MISMATCH!>x<!>
    }
}

fun test10(x:Any?) {
    if (x !is A2 || x is B2) {
        var k: B2 = <!TYPE_MISMATCH!>x<!>
        var k2: A2 = <!TYPE_MISMATCH!>x<!>
        var k3: C2 = <!TYPE_MISMATCH!>x<!>
        var k4: Any? = x
    }
    else {
        var k: Any = x
        var k2: A2 = x
        var k3: B2 = <!TYPE_MISMATCH!>x<!>
        var k4 : C2 = <!TYPE_MISMATCH!>x<!>
    }
}

fun test11(x:A2?) {
    if (x !is B2 || <!USELESS_IS_CHECK!>x is B2<!>) {
        var k: A2? = x
    }
    else {
        var k : B2 = x
        var k2 : A2? = x
    }
}