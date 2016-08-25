fun String.k(): () -> String = { -> this }

fun test1(f: () -> Unit) = f()
fun test2(f: String.() -> Unit) = "hello".f()
fun test3() = "hello".k()()
fun test4(ns: String?) = ns?.k()?.invoke()