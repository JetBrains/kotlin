// "Replace scope function with safe (?.) call" "false"
// WITH_RUNTIME
// ACTION: Add non-null asserted (!!) call
// ACTION: Introduce local variable
// ACTION: Replace with safe (?.) call
// ACTION: Surround with null check
// ERROR: Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type String?

fun foo(a: String?, b: String?) {
    a.let { s ->
        b<caret>.length
    }
}