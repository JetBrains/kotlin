class Klass {
    constructor(a: Int) {}
    constructor(a: String) {}
}

fun user(f: (Int) -> Klass) {}

fun fn() {
    user(::Klass)
}
