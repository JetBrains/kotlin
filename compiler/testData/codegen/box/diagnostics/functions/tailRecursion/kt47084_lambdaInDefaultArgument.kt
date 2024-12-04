fun f(block: () -> Unit): String {
    block()
    return "OK"
}

tailrec fun foo(a: String = run { f {} }): String =
    if (a.length == 0) foo() else a

fun box(): String = foo()
