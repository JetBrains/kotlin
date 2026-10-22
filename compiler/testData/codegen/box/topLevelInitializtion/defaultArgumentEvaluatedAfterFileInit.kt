// FILE: another.kt

private val prop = run {
    log("prop")
}

fun foo(unused: Unit = boo()) {
    log("foo")
}

// FILE: util.kt

var l = ""
fun log(a: String) {
    l += a + ";"
}

fun boo() {
    log("boo")
}

// FILE: main.kt

fun box(): String {
    log("S")
    foo()
    log("E")

    if (l != "S;prop;boo;foo;E;") return l
    return "OK"
}
