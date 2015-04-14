class <caret>C(p1: String, p2: String) {
    public constructor(p: String) : this(p, p) {}
}

fun foo() {
    C("")
    C("1", "2")
}