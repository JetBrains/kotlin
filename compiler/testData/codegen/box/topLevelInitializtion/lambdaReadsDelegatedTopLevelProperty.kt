// WITH_STDLIB
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
    val first = t.action()
    val second = t.action()
    log("E")

    if (first != "lazy value") return "Fail first: $first"
    if (second != "lazy value") return "Fail second: $second"
    if (l != "S;Foo init;M;top-level prop;lazy;E;") return l
    return "OK"
}

// FILE: another.kt

private val other = run {
    log("top-level prop")
}

private val prop by lazy {
    log("lazy")
    "lazy value"
}

class Foo {
    init {
        log("Foo init")
    }
    val action: () -> String = { prop }
}
