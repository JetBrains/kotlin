// !DUMP_CFG
// CONTAINS ERRORS

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

class QImpl(override val data: MyData?) : Q {
    override fun fdata(): MyData? = null
}

class QImplMutable(override var data: MyData?) : Q {
    override fun fdata(): MyData? = null
}

class QImplWithCustomGetter : Q {
    override val data: MyData?
        get() = null

    override fun fdata(): MyData? = null
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
    // `q.data` is a property that has an open getter, so we can NOT smartcast it to non-nullable MyData.
    if (q?.data?.s?.inc() != null) {
        q.data // good
        q.data.<!INAPPLICABLE_CANDIDATE!>s<!> // should be bad
        q.data.<!INAPPLICABLE_CANDIDATE!>s<!>.<!UNRESOLVED_REFERENCE!>inc<!>() // should be bad
    }
}

fun test_6(q: Q?) {
    // `q.data` is a property that has an open getter, so we can NOT smartcast it to non-nullable MyData.
    q?.data?.s?.inc() ?: return
    q.data // good
    q.data.<!INAPPLICABLE_CANDIDATE!>s<!> // should be bad
    q.data.<!INAPPLICABLE_CANDIDATE!>s<!>.<!UNRESOLVED_REFERENCE!>inc<!>() // should be bad
}

fun test_7(q: Q?) {
    if (q?.fdata()?.fs()?.inc() != null) {
        q.fdata() // good
        q.fdata().<!INAPPLICABLE_CANDIDATE!>fs<!>() // bad
        q.fdata().<!INAPPLICABLE_CANDIDATE!>fs<!>().<!UNRESOLVED_REFERENCE!>inc<!>() // bad
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
    b.<!INAPPLICABLE_CANDIDATE!>inc<!>()

    if (a === b) {
        b.inc()
    }
    b.<!INAPPLICABLE_CANDIDATE!>inc<!>()

    if (b == a) {
        b.inc()
    }
    b.<!INAPPLICABLE_CANDIDATE!>inc<!>()

    if (b === a) {
        b.inc()
    }
    b.<!INAPPLICABLE_CANDIDATE!>inc<!>()
}

fun test_10(a: Int?, b: Int?) {
    if (a == b) {
        b.<!INAPPLICABLE_CANDIDATE!>inc<!>()
    }
    b.<!INAPPLICABLE_CANDIDATE!>inc<!>()

    if (a === b) {
        b.<!INAPPLICABLE_CANDIDATE!>inc<!>()
    }
    b.<!INAPPLICABLE_CANDIDATE!>inc<!>()

    if (b == a) {
        b.<!INAPPLICABLE_CANDIDATE!>inc<!>()
    }
    b.<!INAPPLICABLE_CANDIDATE!>inc<!>()

    if (b === a) {
        b.<!INAPPLICABLE_CANDIDATE!>inc<!>()
    }
    b.<!INAPPLICABLE_CANDIDATE!>inc<!>()
}

fun test_11(q: QImpl?, q2: QImpl) {
    // `q.data` is a property with the default getter, so we CAN smartcast it to non-nullable MyData.
    if (q?.data?.s?.inc() != null) {
        q.data
        q.data.s
        q.data.s.inc()

        // Smartcasting of `q.data` should have no effect on `q2.data`.
        // Issue: Smartcasting of QImpl.data affects all instances
        q2.data
        q2.data.<!INAPPLICABLE_CANDIDATE!>s<!> // should be bad
        q2.data.<!INAPPLICABLE_CANDIDATE!>s<!>.<!UNRESOLVED_REFERENCE!>inc<!>() // should be bad

        if (q2.data != null) {
            q2.data.s
            q2.data.s.inc()
        }
    }
}

fun test_12(q: QImplWithCustomGetter?) {
    // `q.data` is a property that has an open getter, so we can NOT smartcast it to non-nullable MyData.
    if (q?.data?.s?.inc() != null) {
        q.data // good
        q.data.<!INAPPLICABLE_CANDIDATE!>s<!> // should be bad
        q.data.<!INAPPLICABLE_CANDIDATE!>s<!>.<!UNRESOLVED_REFERENCE!>inc<!>() // should be bad
    }
}

fun test_13(q: QImplMutable?) {
    // `q.data` is a property that is mutable, so we can NOT smartcast it to non-nullable MyData.
    if (q?.data?.s?.inc() != null) {
        q.data // good
        q.data.<!INAPPLICABLE_CANDIDATE!>s<!> // should be bad
        q.data.<!INAPPLICABLE_CANDIDATE!>s<!>.<!UNRESOLVED_REFERENCE!>inc<!>() // should be bad
    }
}
