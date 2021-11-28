interface Foo
interface Bar : Foo

fun foo(): Foo? = null
fun bar(): Bar? = null

fun <T : Bar> run(f: () -> T): T = f()

val foo: Foo = run {
    val x = bar()
    if (x == null) throw Exception()
    <!DEBUG_INFO_SMARTCAST!>x<!>
}

val foofoo: Foo = run {
    val x = foo()
    if (x == null) throw Exception()
    <!DEBUG_INFO_SMARTCAST, TYPE_MISMATCH!>x<!>
}

val bar: Bar = <!TYPE_MISMATCH!>run {
    val x = foo()
    if (x == null) throw Exception()
    <!DEBUG_INFO_SMARTCAST, TYPE_MISMATCH!>x<!>
}<!>
