fun box(): String {
    if (Parent.a != 1) return "expected Parent.a == 1"
    if (Parent.b != 2) return "expected Parent.b == 2"
    if (Child.a != 1) return "expected Child.a == 1"
    if (Child.b != 3) return "expected Child.b == 3"
    if (Child.c != 4) return "expected Child.c == 4"

    return "OK"
}
