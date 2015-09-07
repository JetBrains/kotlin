package dependency

@Deprecated("", ReplaceWith("s.newFun()"))
fun oldFun(s: String) {}

fun String.newFun() {}
