val f1: () -> Unit = {}
val f2 = {}
val f3: String.() -> String = { this }
fun <T> f4(): Int.(T) -> String = { "" }
val f5 = fun(x:Int) { return "$x" }
val f6 = fun() { 56 }
val f7: () -> Unit = fun() { f1() }