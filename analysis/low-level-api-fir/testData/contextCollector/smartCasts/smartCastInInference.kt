fun test() {
    var a: Any? = null
    if (a == null) return

    var c = id(a)
    <expr>c</expr>
}

fun <T> id(a: T): T = a