// "Add non-null asserted (!!) call" "true"

operator fun Int.get(row: Int, column: Int) = this
fun foo(arg: Int?) = arg<caret>[42, 13]
/* IGNORE_FIR */