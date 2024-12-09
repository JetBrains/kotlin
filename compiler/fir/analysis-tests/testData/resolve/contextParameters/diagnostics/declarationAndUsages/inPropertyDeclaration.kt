// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

fun <A, R> context(context: A, block: context(A) () -> R): R = block(context)

class A {
    fun foo(a: String): String { return a }
    fun usage1() {
        prop1
        prop2
    }
}

context(c: A)
var prop1: String
    get() = c.foo("")
    set(value) { c.foo("") }


context(c: A)
val prop2: String
    get() = c.foo("")

fun usage2(c: A){
    context(c) {
        prop1
        prop2
    }
}

fun usage3(c: A) {
    with(c) {
        prop1
        prop2
    }
}

context(c: A)
fun usage4() {
    prop1
    prop2
}

