// !LANGUAGE: -BooleanElvisBoundSmartCasts

interface Order {
    val expired: Boolean?

    fun notExpired(): Boolean

    fun doSomething()
}

fun foo(o: Any) {
    val order = o as? Order
    if (order?.expired ?: false) {
        order<!UNSAFE_CALL!>.<!>doSomething()
    }
    else {

    }
    if (order?.notExpired() ?: false) {
        order<!UNSAFE_CALL!>.<!>doSomething()
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
        o<!UNSAFE_CALL!>.<!>hashCode()
    }
    if (o ?: true) {

    }
    else {
        o<!UNSAFE_CALL!>.<!>hashCode()
    }
}