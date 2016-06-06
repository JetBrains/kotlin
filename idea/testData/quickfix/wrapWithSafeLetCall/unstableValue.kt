// "Wrap with '?.let { ... }' call" "false"
// WITH_RUNTIME
// ACTION: Add non-null asserted (!!) call
// ACTION: Introduce local variable
// ACTION: Replace with safe (?.) call
// ERROR: Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type Int?

class My(var x: Int?) {

    fun foo() {
        x<caret>.hashCode()
    }
}