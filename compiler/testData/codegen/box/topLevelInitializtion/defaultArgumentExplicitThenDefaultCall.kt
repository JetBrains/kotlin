// FILE: another.kt

private val prop = run {
    log("prop")
}

fun foo(x: String = boo()) {
    log("foo:$x")
}

// FILE: util.kt

var l = ""
fun log(a: String) {
    l += a + ";"
}

fun boo(): String {
    log("boo")
    return "b"
}

// FILE: main.kt

fun box(): String {
    log("S")
    foo("explicit")
    foo()
    foo()
    log("E")

    if (l != "S;prop;foo:explicit;boo;foo:b;boo;foo:b;E;") return l
    return "OK"
}
