// "Wrap with '?.let { ... }' call" "false"
// WITH_RUNTIME
// ACTION: Add non-null asserted (!!) call
// ACTION: Replace with safe (?.) call
// ACTION: Convert to run
// ACTION: Convert to with
// ERROR: Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type Int?

fun Int.bar() {}

class My(var x: Int?) {

    fun foo() {
        x<caret>.bar()
    }
}