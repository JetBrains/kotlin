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
        x.inc()
        y.<!INAPPLICABLE_CANDIDATE!>inc<!>()
    }

    if (y != null) {
        x.<!INAPPLICABLE_CANDIDATE!>inc<!>()
        y.inc()
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
        x.inc()
        y.<!INAPPLICABLE_CANDIDATE!>inc<!>()
    }

    if (y != null) {
        x.<!INAPPLICABLE_CANDIDATE!>inc<!>()
        y.inc()
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
        x.inc()
        y.<!INAPPLICABLE_CANDIDATE!>inc<!>()
    }

    if (y != null) {
        x.<!INAPPLICABLE_CANDIDATE!>inc<!>()
        y.inc()
    }
}

fun test3(x: Int?) {
    val y = try {
        x
    } catch (e: Exception) {
        return
    }

    if (x != null) {
        x.inc()
        y.<!INAPPLICABLE_CANDIDATE!>inc<!>()
    }

    if (y != null) {
        x.<!INAPPLICABLE_CANDIDATE!>inc<!>()
        y.inc()
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
        x.inc()
        y.<!INAPPLICABLE_CANDIDATE!>inc<!>()
    }

    if (y != null) {
        x.<!INAPPLICABLE_CANDIDATE!>inc<!>()
        y.inc()
    }
}
