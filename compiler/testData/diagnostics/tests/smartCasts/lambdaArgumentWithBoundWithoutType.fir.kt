// !WITH_NEW_INFERENCE
interface Foo
interface Bar : Foo

fun foo(): Foo? = null
fun bar(): Bar? = null

fun <T : Bar> run(f: () -> T): T = f()

val foo: Foo = run {
    val x = bar()
    if (x == null) throw Exception()
    x
}

val foofoo: Foo = run {
    val x = foo()
    if (x == null) throw Exception()
    x
}

val bar: Bar = <!INITIALIZER_TYPE_MISMATCH!>run {
    val x = foo()
    if (x == null) throw Exception()
    x
}<!>
