fun test() {
    Parent.a
    Parent.b
    Parent.foo()
    Parent.baz()

    Child.a
    Child.b
    Child.c
    Child.foo()
    Child.bar()
    Child.baz()
}

// @TestKt.class:
// 1 GETSTATIC Parent.a : I
// 1 GETSTATIC Parent.b : I
// 1 INVOKESTATIC Parent.foo()
// 1 INVOKESTATIC Parent.baz()
// 1 GETSTATIC Child.a : I
// 1 GETSTATIC Child.b : I
// 1 GETSTATIC Child.c : I
// 1 INVOKESTATIC Child.foo()
// 1 INVOKESTATIC Child.bar()
// 1 INVOKESTATIC Child.baz()
