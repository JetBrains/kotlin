// RUN_PIPELINE_TILL: BACKEND
interface Foo
fun foo(): Foo? = null

val foo: Foo = run {
    run {
        val x = foo()
        if (x == null) throw Exception()
        x
    }
}
