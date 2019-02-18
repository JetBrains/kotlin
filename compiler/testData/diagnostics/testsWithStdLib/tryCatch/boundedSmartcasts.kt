// !WTIH_NEW_INFERENCE
// SKIP_TXT

class ExcA : Exception()
class ExcB : Exception()

fun test0(x: Int?) {
    val y = try {
        x
    } finally {

    }

    if (x != null) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
        y<!UNSAFE_CALL!>.<!>inc()
    }

    if (y != null) {
        x<!UNSAFE_CALL!>.<!>inc()
        <!DEBUG_INFO_SMARTCAST!>y<!>.inc()
    }
}

fun test1(x: Int?) {
    val y = try {
        x
    }
    catch (e: Exception) {
        42
    }

    if (x != null) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
        y<!UNSAFE_CALL!>.<!>inc()
    }

    if (y != null) {
        x<!UNSAFE_CALL!>.<!>inc()
        <!DEBUG_INFO_SMARTCAST!>y<!>.inc()
    }
}

fun test2(x: Int?) {
    val y = try {
        x
    }
    catch (e: Exception) {
        x
    }

    if (x != null) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
        y<!UNSAFE_CALL!>.<!>inc()
    }

    if (y != null) {
        x<!UNSAFE_CALL!>.<!>inc()
        <!DEBUG_INFO_SMARTCAST!>y<!>.inc()
    }
}

fun test3(x: Int?) {
    val y = try {
        x
    } catch (e: Exception) {
        return
    }

    if (x != null) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
        y<!UNSAFE_CALL!>.<!>inc()
    }

    if (y != null) {
        x<!UNSAFE_CALL!>.<!>inc()
        <!DEBUG_INFO_SMARTCAST!>y<!>.inc()
    }
}

fun test5(x: Int?) {
    val y = try {
        x
    }
    catch (e: ExcA) {
        return
    }
    catch (e: ExcB) {
        x
    }

    if (x != null) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
        y<!UNSAFE_CALL!>.<!>inc()
    }

    if (y != null) {
        x<!UNSAFE_CALL!>.<!>inc()
        <!DEBUG_INFO_SMARTCAST!>y<!>.inc()
    }
}
