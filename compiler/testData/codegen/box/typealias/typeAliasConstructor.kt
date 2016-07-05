class C(val x: String) {
    constructor(s1: String, s2: String) : this(s1 + s2)
}

typealias Alias = C

fun box(): String = Alias("O", "").x + Alias("K").x
