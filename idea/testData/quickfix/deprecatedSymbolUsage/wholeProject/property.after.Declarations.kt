package pack

@Deprecated("", ReplaceWith("newProp"))
val oldProp: String = ""

val String.oldProp: String get() = ""

val newProp: String = ""

fun foo(s: String){}
fun bar(s: String){}
