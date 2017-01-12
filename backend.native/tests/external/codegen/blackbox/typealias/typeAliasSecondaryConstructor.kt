class C(val x: String) {
    constructor(n: Int) : this(n.toString())
}

typealias Alias = C

fun box(): String {
    val c = Alias(23)
    if (c.x != "23") return "fail: $c"
    return "OK"
}
