package foo

class Arg

@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)
const val constant = 0

fun interface Foo {
    fun foo(a: @Anno("foo param type $constant") Arg): @Anno("foo return type $constant") Arg
}

fun testMe(f: @Anno("testMe param type $constant") Foo) {}

<expr>
fun resolveMe() {
    testMe { b -> b }
}
</expr>