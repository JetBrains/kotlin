// "Replace scope function with safe (?.) call" "false"
// WITH_RUNTIME
// ACTION: Add non-null asserted (!!) call
// ACTION: Introduce local variable
// ACTION: Move lambda argument into parentheses
// ACTION: Replace with safe (this?.) call
// ERROR: Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type String?

fun String?.foo(a: String?) {
    a.let { s ->
        <caret>length
    }
}