// RUN_PIPELINE_TILL: FRONTEND
inline fun callsInPlaceInline(x: () -> Unit) = x()

<!NOTHING_TO_INLINE!>inline<!> fun <T> any(x: T) = x

<!NOTHING_TO_INLINE!>inline<!> fun noinline(noinline x: () -> Unit) = x

inline fun crossinline(crossinline x: () -> Unit) = { x() }

fun testInline() {
    var x: String? = null
    callsInPlaceInline { x = null }
    x = ""
    x.length // ok
}

fun testGeneric() {
    var x: String? = ""
    val lambda = any { x = null }
    x = ""
    lambda()
    <!SMARTCAST_IMPOSSIBLE!>x<!>.length // bad
}

fun testNoinline() {
    var x: String? = ""
    val lambda = noinline { x = null }
    x = ""
    lambda()
    <!SMARTCAST_IMPOSSIBLE!>x<!>.length // bad
}

fun testCrossinline() {
    var x: String? = ""
    val lambda = crossinline { x = null }
    x = ""
    lambda()
    <!SMARTCAST_IMPOSSIBLE!>x<!>.length // bad
}
