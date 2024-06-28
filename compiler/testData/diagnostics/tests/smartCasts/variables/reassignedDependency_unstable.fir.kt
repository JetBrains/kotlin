// SKIP_TXT

class C {
    val x: String?
        get() = null
}

fun test1() {
    var c = C()
    val x = c.x
    if (x == null) return
    x.length // smartcast
    c.x<!UNSAFE_CALL!>.<!>length // no smartcast
    c = C()
    x.length // smartcast
    c.x<!UNSAFE_CALL!>.<!>length // no smartcast
}

fun test2() {
    var c = C()
    val x = c.x
    if (x == null) return
    while (true) {
        x.length // smartcast
        c.x<!UNSAFE_CALL!>.<!>length // no smartcast
        c = C()
        x.length // smartcast
        c.x<!UNSAFE_CALL!>.<!>length // no smartcast
    }
}

fun test3(p: Boolean) {
    var c = C()
    val x = c.x
    if (x == null) return
    x.length // smartcast
    c.x<!UNSAFE_CALL!>.<!>length // no smartcast
    if (p) {
        c = C()
    }
    x.length // smartcast
    c.x<!UNSAFE_CALL!>.<!>length // no smartcast
}

fun test4(p: Boolean, q: Boolean) {
    var c = C()
    val x = c.x
    if (x == null) return
    x.length // smartcast
    c.x<!UNSAFE_CALL!>.<!>length // no smartcast
    if (p) {
        if (q) {
            c = C()
        } else {
            c = C()
        }
    } else {
        if (q) {
            c = C()
        } else {
            c = C()
        }
    }
    x.length // smartcast
    c.x<!UNSAFE_CALL!>.<!>length // no smartcast
}

fun test5() {
    var c = C()
    val d = c
    val x = d.x
    if (x == null) return
    x.length // smartcast
    c.x<!UNSAFE_CALL!>.<!>length // no smartcast
    d.x<!UNSAFE_CALL!>.<!>length // no smartcast
    c = C()
    x.length // smartcast
    c.x<!UNSAFE_CALL!>.<!>length // no smartcast
    d.x<!UNSAFE_CALL!>.<!>length // no smartcast
}
