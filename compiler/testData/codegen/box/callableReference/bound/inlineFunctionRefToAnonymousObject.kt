inline fun inlineFun(crossinline lambda: () -> String) =
    object  {
        fun foo(): String = lambda.invoke()
    }.foo()

class A {
    val prop = inlineFun(::bar)
    fun bar() = "OK"
}

fun box():String {
    return A().prop
}
