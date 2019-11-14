interface A {
    fun foo()
    fun getA(): A
}

interface MyData{
    val s: Int

    fun fs(): Int
}

interface Q {
    val data: MyData?

    fun fdata(): MyData?
}

// -------------------------------------------------------------------

fun test_1(x: A?) {
    if (x != null) {
        x.foo()
    } else {
        x.<!INAPPLICABLE_CANDIDATE!>foo<!>()
    }
    x.<!INAPPLICABLE_CANDIDATE!>foo<!>()
}

fun test_2(x: A?) {
    if (x == null) {
        x.<!INAPPLICABLE_CANDIDATE!>foo<!>()
    } else {
        x.foo()
    }
    x.<!INAPPLICABLE_CANDIDATE!>foo<!>()
}

fun test_3(x: A?) {
    x ?: return
    x.foo()
}

fun test_4(x: A?) {
    if (x?.getA() == null) return
    x.foo()
}

fun test_5(q: Q?) {
    if (q?.data?.s?.inc() != null) {
        q.data
        q.data.s
        q.data.s.inc()
    }
}

fun test_6(q: Q?) {
    q?.data?.s?.inc() ?: return
    q.data
    q.data.s
    q.data.s.inc()
}

fun test_7(q: Q?) {
    if (q?.fdata()?.fs()?.inc() != null) {
        q.fdata() // good
        q.fdata().<!INAPPLICABLE_CANDIDATE!>fs<!>() // bad
        q.fdata().<!INAPPLICABLE_CANDIDATE!>fs<!>().<!AMBIGUITY!>inc<!>() // bad
    }
}

fun test_8(b: Boolean?) {
    if (b == true) {
        b.not()
    }
}

fun test_9(a: Int, b: Int?) {
    if (a == b) {
        b.inc()
    }
    b.<!AMBIGUITY!>inc<!>()

    if (a === b) {
        b.inc()
    }
    b.<!AMBIGUITY!>inc<!>()

    if (b == a) {
        b.inc()
    }
    b.<!AMBIGUITY!>inc<!>()

    if (b === a) {
        b.inc()
    }
    b.<!AMBIGUITY!>inc<!>()
}

fun test_10(a: Int?, b: Int?) {
    if (a == b) {
        b.<!AMBIGUITY!>inc<!>()
    }
    b.<!AMBIGUITY!>inc<!>()

    if (a === b) {
        b.<!AMBIGUITY!>inc<!>()
    }
    b.<!AMBIGUITY!>inc<!>()

    if (b == a) {
        b.<!AMBIGUITY!>inc<!>()
    }
    b.<!AMBIGUITY!>inc<!>()

    if (b === a) {
        b.<!AMBIGUITY!>inc<!>()
    }
    b.<!AMBIGUITY!>inc<!>()
}
