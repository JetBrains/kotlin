// RUN_PIPELINE_TILL: SOURCE
// ISSUE: KT-61182

object Foo

operator fun Foo.invoke(f: () -> Unit) {
    f()
}

fun test(g: () -> Int) {
    Foo(g)
}