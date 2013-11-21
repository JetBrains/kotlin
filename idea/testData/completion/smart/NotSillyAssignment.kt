class Foo(val prop : String)

fun f(foo1 : Foo, foo2 : Foo) {
    foo1.prop = foo2.<caret>
}

// EXIST: prop
