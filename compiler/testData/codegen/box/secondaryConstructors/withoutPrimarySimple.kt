class A {
    val value: String
        get() = field + "K"

    constructor(o: String) {
        value = o
    }
}

fun box() = A("O").value