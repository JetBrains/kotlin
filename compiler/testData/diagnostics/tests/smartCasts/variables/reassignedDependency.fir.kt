// SKIP_TXT

class C(val x: String?)

fun test1() {
    var c = C("...")
    val x = c.x
    if (x == null) return
    x.length // ok
    c.x.length // ok
    c = C(null)
    x.length // ok
    c.x<!UNSAFE_CALL!>.<!>length // bad
}

fun test2() {
    var c = C("...")
    val x = c.x
    if (x == null) return
    while (true) {
        x.length // ok
        c.x<!UNSAFE_CALL!>.<!>length // bad
        c = C(null)
        x.length // ok
        c.x<!UNSAFE_CALL!>.<!>length // bad
    }
}

fun test3(p: Boolean) {
    var c = C("...")
    val x = c.x
    if (x == null) return
    x.length // ok
    c.x.length // ok
    if (p) {
        c = C(null)
    }
    x.length // ok
    c.x<!UNSAFE_CALL!>.<!>length // bad
}

fun test4(p: Boolean, q: Boolean) {
    var c = C("...")
    val x = c.x
    if (x == null) return
    x.length // ok
    c.x.length // ok
    if (p) {
        if (q) {
            c = C(null)
        } else {
            c = C(null)
        }
    } else {
        if (q) {
            c = C(null)
        } else {
            c = C(null)
        }
    }
    x.length // ok
    c.x<!UNSAFE_CALL!>.<!>length // bad
}

fun test5() {
    var c = C("...")
    val d = c
    val x = d.x
    if (x == null) return
    x.length // ok
    c.x.length // ok
    d.x.length // ok
    c = C(null)
    x.length // ok
    c.x<!UNSAFE_CALL!>.<!>length // bad
    d.x.length // ok
}

fun test6() {
    var c: C? = null
    var maybeC: C? = C("")
    if (c == null) {
        c = maybeC ?: throw Exception()
    }
    c.x
}
