// SKIP_TXT

class C(val x: String?)

fun test1() {
    var c = C("...")
    val x = c.x
    if (x == null) return
    x.length // ok
    c.x.length // ok
    c = C(null)
    x<!UNSAFE_CALL!>.<!>length // ok
    c.x<!UNSAFE_CALL!>.<!>length // bad
}

fun test2() {
    var c = C("...")
    val x = c.x
    if (x == null) return
    while (true) {
        x<!UNSAFE_CALL!>.<!>length // ok
        c.x<!UNSAFE_CALL!>.<!>length // bad
        c = C(null)
        x<!UNSAFE_CALL!>.<!>length // ok
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
    c.x.length // bad
}
