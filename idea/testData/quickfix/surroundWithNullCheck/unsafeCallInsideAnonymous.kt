// "Surround with null check" "false"
// WITH_RUNTIME
// ACTION: Add 'block =' to argument
// ACTION: Add non-null asserted (!!) call
// ACTION: Convert to block body
// ACTION: Introduce local variable
// ACTION: Replace with safe (?.) call
// ERROR: Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type Int?

fun foo(arg: Int?) {
    run(fun() = arg<caret>.hashCode())
}