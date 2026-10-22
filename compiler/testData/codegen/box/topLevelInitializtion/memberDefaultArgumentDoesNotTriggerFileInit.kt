// FILE: another.kt

private val prop = run {
    log("prop")
}

class Foo(unused: Unit = boo()) {
    init {
        log("Foo init")
    }

    fun bar(unused: Unit = boo()) {
        log("bar")
    }
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
    Foo().bar()
    log("E")

    if (l != "S;boo;Foo init;boo;bar;E;") return l
    return "OK"
}
