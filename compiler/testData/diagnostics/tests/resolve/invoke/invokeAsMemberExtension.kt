class Foo

//no variable
trait A {
    fun Foo.invoke() {}

    fun test(foo: Foo) {
        foo()
    }
}

//variable as member
trait B {
    val foo: Foo
}

class C {
    fun Foo.invoke() {}

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
trait D {
}
val D.foo = Foo()

class E {
    fun Foo.invoke() {}

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
trait F

trait G {
    val F.foo: Foo
    fun Foo.invoke()

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
trait X

trait U {
    val X.foo: Foo
}

trait V {
    fun Foo.invoke() {}

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

//--------------
fun <T, R> with(receiver: T, f: T.() -> R) : R = receiver.f()