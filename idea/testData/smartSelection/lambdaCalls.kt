interface T {
    fun foo(a: () -> Any): T = this
}

fun foo(t: T) {
    t.foo { <caret>1 }
    .foo {2}
            .foo({ 3 })
}

/*
1
{ 1 }
t.foo{...}
t.foo{...}.foo{...}
t.foo{...}.foo{...}.foo(...)
*/