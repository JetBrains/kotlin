//KT-3631 String.invoke doesn't work with literals

fun String.invoke(i: Int) = "$this$i"

fun box() = if ("a"(12) == "a12") "OK" else "fail"