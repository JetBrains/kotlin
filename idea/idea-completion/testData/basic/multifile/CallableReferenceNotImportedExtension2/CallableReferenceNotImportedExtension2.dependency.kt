package dependency

fun CharSequence.extFun(){}

val String.extVal: Int get() = 1

fun Int.wrongExtFun(){}
