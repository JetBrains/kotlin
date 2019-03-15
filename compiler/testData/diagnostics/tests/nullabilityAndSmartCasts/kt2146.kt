// !WITH_NEW_INFERENCE
//KT-2146 Nullability casts in when.
package kt2146

fun f1(s: Int?): Int {
    return when (s) {
        null -> 3
        else -> <!DEBUG_INFO_SMARTCAST!>s<!>
    }
}

fun f2(s: Int?): Int {
    return <!NI;TYPE_MISMATCH!>when (s) {
        !is Int -> <!OI;TYPE_MISMATCH!>s<!>
        else -> <!DEBUG_INFO_SMARTCAST!>s<!>
    }<!>
}

fun f3(s: Int?): Int {
    return <!NI;TYPE_MISMATCH!>when (s) {
        is Int -> <!DEBUG_INFO_SMARTCAST!>s<!>
        else -> <!OI;TYPE_MISMATCH!>s<!>
    }<!>
}

fun f4(s: Int?): Int {
    return <!NI;TYPE_MISMATCH!>when {
        s == 4 -> <!DEBUG_INFO_SMARTCAST!>s<!>
        s == null -> <!DEBUG_INFO_CONSTANT, OI;TYPE_MISMATCH!>s<!>
        else -> <!DEBUG_INFO_SMARTCAST!>s<!>
    }<!>
}

fun f5(s: Int?): Int {
    return <!NI;TYPE_MISMATCH!>when (s) {
        s -> <!OI;TYPE_MISMATCH!>s<!>
        s!! -> <!DEBUG_INFO_SMARTCAST!>s<!>
        s -> <!DEBUG_INFO_SMARTCAST!>s<!>
        else -> 0
    }<!>
}

fun f6(s: Int?): Int {
    return <!NI;TYPE_MISMATCH!>when {
        s is Int -> <!DEBUG_INFO_SMARTCAST!>s<!>
        else -> <!OI;TYPE_MISMATCH!>s<!>
    }<!>
}

fun f7(s: Int?): Int {
    return <!NI;TYPE_MISMATCH!>when {
        s !is Int -> <!OI;TYPE_MISMATCH!>s<!>
        else -> <!DEBUG_INFO_SMARTCAST!>s<!>
    }<!>
}
