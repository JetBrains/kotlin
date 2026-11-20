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
    t.action()
    log("E")

    if (l != "S;Foo init;M;Foo lambda;E;") return l
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
    val action: () -> Unit = {
        log("Foo lambda")
    }
}
