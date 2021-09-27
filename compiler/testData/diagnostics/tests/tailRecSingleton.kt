object Foo {
    tailrec fun foo1() {
        foo1()
    }

    tailrec fun foo2() {
        this.foo2()
    }

    tailrec fun foo3() {
        Foo.foo3()
    }
}

class Bar {
    companion object {
        tailrec fun bar1() {
            bar1()
        }

        tailrec fun bar2() {
            this.bar2()
        }

        <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun bar3() {
            Bar.<!NON_TAIL_RECURSIVE_CALL!>bar3<!>()
        }

        tailrec fun bar4() {
            Bar.Companion.bar4()
        }
    }
}

enum class E {
    A {
        override tailrec fun rec() {
            rec()
        }
    },
    B {
        override tailrec fun rec() {
            this.rec()
        }
    },
    C {
        override <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun rec() {
            C.rec() // resolution goes to `E.rec`. Hence the resolved symbol is considered different from `C.rec`.
        }
    };

    abstract fun rec()
}
