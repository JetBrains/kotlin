package foo.bar

class A

object F {
    class foo {
        class bar {
            annotation class A

            @foo.bar.A fun test(): Nothing = TODO()
        }
    }
}

// ANNOTATION: foo.bar.F.foo.bar.A
// SEARCH: method:test
