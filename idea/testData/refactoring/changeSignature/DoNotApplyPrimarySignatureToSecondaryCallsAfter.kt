class C(p2: String, p1: String) {
    public constructor(p: String) : this(p, p) {}
}

fun foo() {
    C("")
    C("2", "1")
}