// FILE: another.kt

private val prop = run {
    log("prop")
    "prop value"
}

fun foo(x: String = prop): String {
    log("foo")
    return x
}

fun bar(x: String = boo() + prop): String {
    log("bar")
    return x
}

// FILE: util.kt

var l = ""
fun log(a: String) {
    l += a + ";"
}

fun boo(): String {
    log("boo")
    return "boo + "
}

// FILE: main.kt

fun box(): String {
    log("S")
    val fooResult = foo()
    val barResult = bar()
    log("E")

    if (fooResult != "prop value") return "Fail foo: $fooResult"
    if (barResult != "boo + prop value") return "Fail bar: $barResult"
    if (l != "S;prop;foo;boo;bar;E;") return l
    return "OK"
}
