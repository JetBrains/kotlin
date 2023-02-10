// ISSUE: KT-30756

fun condition(): Boolean = true

fun test_1(x: String?) {
    do {
        x ?: return
    } while(condition())

    x.length
}

fun test_2(x: String?) {
    do {
        x ?: x!!
    } while(condition())

    x.length
}

fun test_3() {
    var a: String? = null

    while (condition()) {
        a ?: return
    }

    a<!UNSAFE_CALL!>.<!>length
}

fun test_4() {
    var a: String? = null

    while (true) {
        a ?: return
    }

    a<!UNSAFE_CALL!>.<!>length
}

fun test_5(x: String?) {
    do {
        x ?: return
        Unit
    } while(condition())

    x.length
}

fun test_6(x: String?) {
    do {
        x ?: x!!
        Unit
    } while(condition())

    x.length
}

fun test_7() {
    var a: String? = null

    while (condition()) {
        a ?: return
        a
    }

    a<!UNSAFE_CALL!>.<!>length
}

fun test_8() {
    var a: String? = null

    while (true) {
        a ?: return
        a
    }

    a<!UNSAFE_CALL!>.<!>length
}
