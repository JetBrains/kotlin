fun testInvoke() {
    operator fun Nothing.invoke(): Nothing = this
    todo()()
}

fun testInvokeWithLambda() {
    operator fun Nothing.invoke(i: Int, f: () -> Int) = f
    todo()(1){ 42 }
}

fun todo(): Nothing = throw Exception()