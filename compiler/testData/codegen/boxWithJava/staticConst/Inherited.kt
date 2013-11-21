fun box(): String {
    if (Derived.FOO != "initial") return "Fail 1: ${Derived.FOO}"

    Derived.FOO = "foo"
    if (Derived.FOO != "foo") return "Fail 2: ${Derived.FOO}"

    return "OK"
}