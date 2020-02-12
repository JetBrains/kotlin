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
        y.<!INAPPLICABLE_CANDIDATE!>length<!> // Bad
        z.length // OK
    }
    if (y != null) {
        x.<!INAPPLICABLE_CANDIDATE!>length<!> // Bad
        y.length // OK
        z.<!INAPPLICABLE_CANDIDATE!>length<!> // Bad
    }

    if (z != null) {
        x.length // OK
        y.<!INAPPLICABLE_CANDIDATE!>length<!> // Bad
        z.length // OK
    }
}