// !LANGUAGE: -BooleanElvisBoundSmartCasts

interface Order {
    val expired: Boolean?

    fun notExpired(): Boolean

    fun doSomething()
}

fun foo(o: Any) {
    val order = o as? Order
    if (order?.expired ?: false) {
        order.<!INAPPLICABLE_CANDIDATE!>doSomething<!>()
    }
    else {

    }
    if (order?.notExpired() ?: false) {
        order.<!INAPPLICABLE_CANDIDATE!>doSomething<!>()
    }
}

fun bar(o: Any) {
    val order = o as? Order
    if (order?.expired ?: true) {

    }
    else {
        order!!.doSomething()
    }
    if (order?.notExpired() ?: true) {

    }
    else {
        order!!.doSomething()
    }
}

fun baz(o: Boolean?) {
    if (o ?: false) {
        o.<!INAPPLICABLE_CANDIDATE!>hashCode<!>()
    }
    if (o ?: true) {

    }
    else {
        o.<!INAPPLICABLE_CANDIDATE!>hashCode<!>()
    }
}