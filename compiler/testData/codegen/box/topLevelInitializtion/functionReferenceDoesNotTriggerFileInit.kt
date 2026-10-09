// FILE: log.kt

var l = ""
fun log(a: String) {
    l += a + ";"
}

// FILE: main.kt

fun box(): String {
    log("S")
    val t = Foo()
    log("M")
    t.reference()
    log("E")

    if (l != "S;Foo init;M;target prop;target;E;") return l
    return "OK"
}

// FILE: another.kt

private val prop = run {
    log("top-level prop")
}

class Foo {
    init {
        log("Foo init")
    }
    val reference: () -> Unit = ::target
}

// FILE: target.kt

private val targetProp = run {
    log("target prop")
}

fun target() {
    log("target")
}
