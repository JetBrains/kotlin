fun dynamic.foo()
fun dynamic?.foo()
val dynamic.foo: Int
val dynamic?.foo: Int

val foo: dynamic.() -> Unit

// testing look-ahead with comments and whitespace

fun dynamic . foo()
fun dynamic
        .foo()
fun dynamic// line-comment
        .foo()
fun dynamic/*
*/.foo()
