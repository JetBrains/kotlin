// "Surround with null check" "false"
// ACTION: Add non-null asserted (!!) call
// ACTION: Replace with safe (?.) call
// ACTION: Convert to run
// ACTION: Convert to with
// ERROR: Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type String?

fun foo(s: String?) {
    val x = s<caret>.hashCode()
}