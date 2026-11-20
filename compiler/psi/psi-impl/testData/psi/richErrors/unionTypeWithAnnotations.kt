// COMPILATION_ERRORS
error class Foo

@Target(AnnotationTarget.TYPE)
annotation class Ann

fun foo1(): @Ann String | Foo = ""
fun <T> foo2(t: T): @Ann T & Any | Foo = ""
fun foo3(): @Ann (suspend () -> Unit) | Foo = {}

fun foo4(): @Ann (@Ann String | Foo) = ""
fun <T> foo5(t: T & Any): @Ann (@Ann T & Any | Foo) = t
fun foo6(): @Ann (@Ann (suspend () -> Unit) | Foo) = suspend {}
