// "Add parameter to function 'bar'" "true"
// DISABLE-ERRORS
fun bar(isObject: Boolean) {}

fun test(isObject: Boolean) {
    bar(true, isObject<caret>)
}