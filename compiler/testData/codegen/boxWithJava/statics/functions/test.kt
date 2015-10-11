fun box(): String {
    if (Parent.foo() != "Parent.foo") return "expected: Parent.foo"
    if (Parent.baz() != "Parent.baz") return "expected: Parent.baz"
    if (Child.foo() != "Parent.foo") return "expected: Child.foo() != Parent.foo"
    if (Child.baz() != "Child.baz") return "expected: Child.baz"
    if (Child.bar() != "Child.bar") return "expected: Child.bar"

    return "OK"
}
