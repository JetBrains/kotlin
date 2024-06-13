// LANGUAGE: +BooleanElvisBoundSmartCasts

interface Order {
    val expired: Boolean?

    fun notExpired(): Boolean

    fun doSomething()
}

fun foo(o: Any) {
    val order = o as? Order
    if (order?.expired ?: false) {
        order.doSomething()
    }
    else {

    }
    if (order?.notExpired() ?: false) {
        order.doSomething()
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
        o.hashCode()
    }
    if (o ?: true) {

    }
    else {
        o.hashCode()
    }
}
