// "Create class 'A!u00A0'" "true"
// SHOULD_FAIL_WITH: java.util.concurrent.ExecutionException: java.lang.AssertionError: java.lang.AssertionError
fun test() {
    val t = <caret>`A!u00A0`(1)
}