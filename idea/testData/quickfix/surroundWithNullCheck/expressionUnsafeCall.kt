// "Surround with null check" "false"
// ACTION: Add non-null asserted (!!) call
// ACTION: Convert to block body
// ACTION: Introduce local variable
// ACTION: Replace with safe (?.) call
// ACTION: Convert to run
// ACTION: Convert to with
// ERROR: Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type Int?

fun foo(arg: Int?) = arg<caret>.hashCode()