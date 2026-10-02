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
    val first = t.increment()
    val second = t.increment()
    log("E")

    if (first != 11) return "Fail first: $first"
    if (second != 12) return "Fail second: $second"
    if (l != "S;Foo init;M;top-level prop;E;") return l
    return "OK"
}

// FILE: another.kt

private var counter = run {
    log("top-level prop")
    10
}

class Foo {
    init {
        log("Foo init")
    }
    val increment: () -> Int = {
        counter += 1
        counter
    }
}
