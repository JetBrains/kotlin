// PROBLEM: none
// WITH_RUNTIME

fun test() {
    val predicate = { _: String -> true }
    "".let {<caret> predicate::invoke }("123")
}