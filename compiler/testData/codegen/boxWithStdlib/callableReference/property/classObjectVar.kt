class A {
    default object B {
        var state: String = "12345"
    }
}

fun box(): String {
    val p = A.B::state

    if (p.name != "state") return "Fail state: ${p.name}"
    if (p.get(A.B) != "12345") return "Fail value: ${p.get(A.B)}"
    p.set(A.B, "OK")

    return p[A.B]
}
