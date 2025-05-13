// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

context(a: T)
fun <T> foo(b: T) {}

context(a: T & Any)
fun <T> bar(b: T) {}

interface A
interface B
class C : A, B
context(a: T)
fun <T> baz() where T: A, T: B {}

context(from: Array<out T>, to: Array<in K>, x: Array<*>)
fun <T, K> qux(a: T, b: K) {
    from.set(1, <!MEMBER_PROJECTED_OUT!>a<!>)
    x.set(0, <!MEMBER_PROJECTED_OUT!>a<!>)
    to.set(0, b)
}

fun <T> quux(a: context(T) () -> T, b: T) {
    a(b)
}

fun usage() {
    with("") {
        foo("")
        bar(null)
    }
    with(C()) {
        baz()
    }
    context(arrayOf("")) {
        with(arrayOf(1)) {
            with(arrayOf(1.2)){
                qux("", 1)
            }
        }
    }
}