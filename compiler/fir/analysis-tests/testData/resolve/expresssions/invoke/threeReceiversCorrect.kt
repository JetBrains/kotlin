// RUN_PIPELINE_TILL: FRONTEND
class C

class B

class A {
    val B.foo: C.() -> Unit get() = <!NULL_FOR_NONNULL_TYPE("Function1<C, Unit>")!>null<!>
}

fun <T, R> with(arg: T, f: T.() -> R): R = arg.f()

fun test(a: A, b: B, c: C) {
    with(a) {
        with(c) {
            b.foo(c)
            // [this@a,b].foo.invoke(c)
        }
        with(b) {
            c.foo()
            // [this@a,this@b].foo.invoke(c)
            with(c) {
                foo()
                // [this@a,this@b].foo.invoke(this@c)
            }
        }
    }
}
