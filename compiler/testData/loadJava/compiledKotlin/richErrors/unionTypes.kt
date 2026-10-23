// LANGUAGE: +RichErrors
package test

error class Foo
error object Bar

class Outer<T> {
    inner class Inner<R>
}

@Target(AnnotationTarget.TYPE)
annotation class Ann

fun foo1(): @Ann (@Ann String | Foo | Bar) = ""
fun <X, Y> foo2(): @Ann (@Ann Outer<X>.Inner<Y>? | Foo | Bar) = null
fun foo3(): @Ann ((@Ann suspend () -> Unit) | Foo | Bar) = suspend {}
fun <T> foo4(t: T & Any): @Ann ((@Ann T) & Any | Foo | Bar) = t
