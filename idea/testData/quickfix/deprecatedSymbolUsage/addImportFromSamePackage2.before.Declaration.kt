package dependency

@deprecated("", ReplaceWith("s.extension().newFun()", "dependency2.extension"))
fun oldFun(s: String) {}

fun String.newFun() {}
