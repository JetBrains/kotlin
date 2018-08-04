// WITH_RUNTIME
// FIX: none
fun test(x: Int) {
    x.run {<caret> String::length }
}
