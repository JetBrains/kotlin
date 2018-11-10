// PROBLEM: none
// ERROR: No value passed for parameter 'x'
annotation class MyAnnotation(val x: Int)

// there is an error but the inspection is not triggered because parentheses are needed in the end
@MyAnnotation(<caret>)
fun test() {

}