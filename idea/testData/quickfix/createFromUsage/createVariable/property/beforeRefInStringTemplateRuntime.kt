// "Create property 'foo'" "true"
// ERROR: Property must be initialized

fun test() {
    println("a = $<caret>foo")
}