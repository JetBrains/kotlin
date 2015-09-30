package second

fun String.extensionFun(){}
val Int.extensionVal: Int get() = 1

fun topLevelFun(p: (String, Int) -> Unit){}
val topLevelVal: Int = 1
