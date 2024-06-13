// DIAGNOSTICS: -UNUSED_PARAMETER

fun <K> id(it: K) = it

fun <E> smartCast(arg: E?, fn: () -> Any?): E = TODO()
fun <E1> noSmartCast1(arg: E1?, fn: () -> E1): E1 = TODO()
fun <E2> noSmartCast2(arg: E2?, fn: E2): E2 = TODO()
fun <E3, F : E3> noSmartCast3(arg: E3?, fn: () -> F): E3 = TODO()
fun <E4, F : E4> noSmartCast4(arg: E4?, fn: F): E4 = TODO()


fun testSmartCast(s: String?) {
    id(
        if (s != null) ""
        else smartCast(null) { "" }
    )
    s.length
}

fun testNoSmartCast1(s: String?) {
    id(
        if (s != null) ""
        else noSmartCast1(null) { "" }
    )
    s<!UNSAFE_CALL!>.<!>length
}

fun testNoSmartCast2(s: String?) {
    id(
        if (s != null) ( {""} )
        else noSmartCast2(null) { "" }
    )
    s<!UNSAFE_CALL!>.<!>length
}

fun testNoSmartCast3(s: String?) {
    id(
        if (s != null) ""
        else noSmartCast3(null) { "" }
    )
    s<!UNSAFE_CALL!>.<!>length
}

// KT-36069
fun testNoSmartCast4(s: String?) {
    id(
        if (s != null) ( {""} )
        else noSmartCast4(null) { "" }
    )
    s<!UNSAFE_CALL!>.<!>length
}
