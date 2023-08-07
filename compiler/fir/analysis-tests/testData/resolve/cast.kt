val x = 1
val y = 2 as Any

val f = fun() = 3 as Any
val g = {}
val h: (String) -> Boolean = { _ -> false }
val hError = { <!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>_<!> -> true }
