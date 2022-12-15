// !LANGUAGE: +ContextReceivers
// LANGUAGE: +LightweightLambdas
// TARGET_BACKEND: JVM_IR

fun box(): String {
    val a = ::meth
    return "OK"
}

private class Ctx

private class Scope {
    val y = 1
}

private val meth: (context(Ctx) Scope.() -> Unit) = {
    y.toString()
}
