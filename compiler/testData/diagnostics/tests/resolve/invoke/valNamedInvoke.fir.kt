interface A

fun foo(invoke: A.()->Unit, a: A) {
    a.invoke()
}

fun bar(invoke: Any.()->Any, a: Any) {
    a.invoke()
}
