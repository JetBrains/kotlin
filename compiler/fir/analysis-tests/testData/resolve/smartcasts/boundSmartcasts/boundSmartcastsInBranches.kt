// !DUMP_CFG
class A {
    val s: String = ""
}

fun test_0(list: List<A>) {
    var goodA: A? = null
    for (a in list) {
        if (goodA == null) {
            goodA = a
            continue
        }
        goodA.s
    }
}

fun test_1(a: A, b: Boolean) {
    val x: Any
    if (b) {
        x = A()
    } else {
        x = a
    }
    x.s
}

fun test_2(a: Any, b: Boolean) {
    val x: Any
    if (b) {
        //x1
        x = A()
    } else {
        //x2 = a
        x = a
        a as A
    }
    x.s
}

fun test_3(a: Any, b: Boolean) {
    val x: Any
    if (b) {
        x = A()
    } else {
        a as A
        x = a
    }
    x.s
}

fun test_4(a: Any, b: Boolean) {
    val x: Any
    if (b) {
        x = a
    } else {
        x = a
    }
    x as A
    x.s
    a.s
}

fun test_5(a: Any, b: Boolean) {
    val x: Any
    if (b) {
        x = a
    } else {
        x = a
    }
    a as A
    x.s
    a.s
}

fun test_6(a: A) {
    val x: Any
    x = a
    x.s
}

fun test_7() {
    val z: String? = null
    var y : String? = z
    val x: String? = y

    if (x != null) {
        x.length // OK
        y<!UNSAFE_CALL!>.<!>length // Bad
        z<!UNSAFE_CALL!>.<!>length // Bad
    }
    if (y != null) {
        x<!UNSAFE_CALL!>.<!>length // Bad
        y.length // OK
        z<!UNSAFE_CALL!>.<!>length // Bad
    }

    if (z != null) {
        x<!UNSAFE_CALL!>.<!>length // Bad
        y<!UNSAFE_CALL!>.<!>length // Bad
        z.length // OK
    }

    y = null

    if (x != null) {
        x.length // OK
        y<!UNSAFE_CALL!>.<!>length // Bad
        z<!UNSAFE_CALL!>.<!>length // Bad
    }
    if (<!SENSELESS_COMPARISON!>y != null<!>) {
        x<!UNSAFE_CALL!>.<!>length // Bad
        y.length // OK
        z<!UNSAFE_CALL!>.<!>length // Bad
    }

    if (z != null) {
        x<!UNSAFE_CALL!>.<!>length // Bad
        y<!UNSAFE_CALL!>.<!>length // Bad
        z.length // OK
    }
}

fun test_8() {
    val z: String? = null
    var y = z
    val x = y

    if (x != null) {
        x.length // OK
        y.length // OK
        z.length // OK
    }
    if (y != null) {
        x.length // OK
        y.length // OK
        z.length // OK
    }

    if (z != null) {
        x.length // OK
        y.length // OK
        z.length // OK
    }

    y = null

    if (x != null) {
        x.length // OK
        y<!UNSAFE_CALL!>.<!>length // Bad
        z.length // OK
    }
    if (<!SENSELESS_COMPARISON!>y != null<!>) {
        x<!UNSAFE_CALL!>.<!>length // Bad
        y.length // OK
        z<!UNSAFE_CALL!>.<!>length // Bad
    }

    if (z != null) {
        x.length // OK
        y<!UNSAFE_CALL!>.<!>length // Bad
        z.length // OK
    }
}

fun test_9() {
    var a: String? = null
    val b: String?
    if (a != null) {
        b = a
    } else {
        b = a
    }
    b<!UNSAFE_CALL!>.<!>length // bad
    if (a != null) {
        b.length // ok
    }
}
