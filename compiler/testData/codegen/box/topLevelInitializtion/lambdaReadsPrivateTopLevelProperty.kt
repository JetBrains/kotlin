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
    val result = t.action()
    log("E")

    if (result != "prop value") return "Fail result: $result"
    if (l != "S;Foo init;M;Foo lambda;top-level prop;E;") return l
    return "OK"
}

// FILE: another.kt

private val prop = run {
    log("top-level prop")
    "prop value"
}

class Foo {
    init {
        log("Foo init")
    }
    val action: () -> String = {
        log("Foo lambda")
        prop
    }
}
