// COMPILATION_ERRORS

val t = context(a: A) fun () { }
val t = context(a: A) fun <A> () { }
val t = @Ann context(a: A) fun () { }
val t = context(a)

fun test(a: Boolean = context(a: Boolean) fun (): Boolean { return a }(true)){}

class A(val k: Boolean = context(a: Boolean) fun (): Boolean { return a }(true))

fun f() {
    when {
        context(a: Boolean) fun(): Boolean { return a }(true) -> 1
        else -> 2
    }

    when(val x = context(a: Boolean) fun(): Boolean { return a }(true)) {
        true -> 1
        else -> 2
    }

    if(x > context(a: Boolean) fun (): Boolean { return a }(true)) { }

    for (i in context(a: Boolean) fun (): IntRange { return 1..10 }(true)){ }
}

val String.t get() = fun1@ context(a: String) fun String.() { this@fun1 + a + this@t }