fun box(): String {
    if (Child.a != "2") return "Fail #1"
    if (Child.foo() != "Child.foo()") return "Fail #2"
    if (Child.foo(1) != "Child.foo(int)") return "Fail #3"

    return "OK"
}
