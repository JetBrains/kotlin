package dependency

@deprecated("", ReplaceWith("s.newFun()"))
fun oldFun(s: String) {}

fun String.newFun() {}
