// !LANGUAGE: +BooleanElvisBoundSmartCasts

interface Order {
    val expired: Boolean?

    fun notExpired(): Boolean

    fun doSomething()
}

fun foo(o: Any) {
    val order = o as? Order
    if (order?.expired ?: false) {
        <!DEBUG_INFO_SMARTCAST!>order<!>.doSomething()
    }
    else {

    }
    if (order?.notExpired() ?: false) {
        <!DEBUG_INFO_SMARTCAST!>order<!>.doSomething()
    }
}

fun bar(o: Any) {
    val order = o as? Order
    if (order?.expired ?: true) {

    }
    else {
        order<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.doSomething()
    }
    if (order?.notExpired() ?: true) {

    }
    else {
        order<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.doSomething()
    }
}

fun baz(o: Boolean?) {
    if (o ?: false) {
        <!DEBUG_INFO_SMARTCAST!>o<!>.hashCode()
    }
    if (o ?: true) {

    }
    else {
        <!DEBUG_INFO_SMARTCAST!>o<!>.hashCode()
    }
}