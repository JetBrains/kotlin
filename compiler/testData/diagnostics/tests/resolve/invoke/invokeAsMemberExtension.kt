class Foo

//no variable
interface A {
    operator fun Foo.invoke() {}

    fun test(foo: Foo) {
        foo()
    }
}

//variable as member
interface B {
    val foo: Foo
}

class C {
    operator fun Foo.invoke() {}

    fun test(b: B) {
        b.foo()

        with (b) {
            foo()
        }
    }
}

fun test(c: C, b: B) {
    with (c) {
        b.foo()

        with (b) {
            foo()
        }
    }
}


//variable as extension,
interface D {
}
val D.foo: Foo
    get() = Foo()

class E {
    operator fun Foo.invoke() {}

    fun test(d: D) {
        d.foo()

        with (d) {
            foo()
        }
    }
}

fun test(e: E, d: D) {
    with (e) {
        d.foo()

        with (d) {
            foo()
        }
    }
}

//variable as member extension
interface F

interface G {
    val F.foo: Foo
    operator fun Foo.invoke()

    fun test(f: F) {
        f.foo()

        with (f) {
            foo()
        }
    }
}

fun test(g: G, f: F) {
    with (g) {
        f.foo()

        with (f) {
            foo()
        }
    }
}

//variable as member extension (2)
interface X

interface U {
    val X.foo: Foo
}

interface V {
    operator fun Foo.invoke() {}

    fun U.test(x: X) {
        x.foo()

        with (x) {
            foo()
        }
    }
}

fun test(u: U, v: V, x: X) {
    with (v) {
        with (u) {
            x.foo()

            with (x) {
                foo()
            }
        }
    }
}
