class Foo {}

operator fun Foo.invoke() {}

//no variable
fun test(foo: Foo) {
    foo()
}

//variable as member
interface A {
    val foo: Foo
}


fun test(a: A) {
    a.foo()

    with (a) {
        foo()
    }
}

//variable as extension
interface B {
}
val B.foo: Foo
    get() = Foo()

fun test(b: B) {
    b.foo()

    with (b) {
        foo()
    }
}

//variable as member extension
interface C

interface D {
    val C.foo: Foo

    fun test(c: C) {
        c.foo()

        with (c) {
            foo()
        }
    }
}

fun test(d: D, c: C) {
    with (d) {
        c.foo()

        with (c) {
            foo()
        }
    }
}
