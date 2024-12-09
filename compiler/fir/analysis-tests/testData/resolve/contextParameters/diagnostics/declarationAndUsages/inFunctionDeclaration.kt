// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

fun <A, R> context(context: A, block: context(A) () -> R): R = block(context)

class A {
    fun foo(a: String): String { return a }
    fun usage1() {
        test()
    }
}

context(c: A)
fun test() {
    c.foo("")
}

fun usage2(c: A) {
    context(c){
        test()
    }
}

fun usage3(c: A) {
    with(c) {
        test()
    }
}

context(c: A)
fun usage4() {
    test()
}

