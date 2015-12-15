interface Foo
fun foo(): Foo? = null

fun <T> run(f: () -> T): T = f()

val foo: Foo = run {
    run {
        val x = foo()
        if (x == null) throw Exception()
        <!DEBUG_INFO_SMARTCAST!>x<!>
    }
}
