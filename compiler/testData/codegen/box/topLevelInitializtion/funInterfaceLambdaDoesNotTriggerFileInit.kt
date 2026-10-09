// FILE: log.kt

var l = ""
fun log(a: String) {
    l += a + ";"
}

// FILE: main.kt

fun box(): String {
    log("S")
    val action = Foo.action
    log("M")
    action.run()
    log("E")

    if (l != "S;Foo init;M;Foo lambda;E;") return l
    return "OK"
}

// FILE: another.kt

private val prop = run {
    log("top-level prop")
}

fun interface Action {
    fun run()
}

object Foo {
    init {
        log("Foo init")
    }
    val action = Action {
        log("Foo lambda")
    }
}
