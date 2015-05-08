// "Create parameter 'foo'" "true"
// ERROR: No value passed for parameter foo
// ERROR: No value passed for parameter foo
// ERROR: No value passed for parameter foo

enum class E(val foo: Int) {
    A : E()
    B : E() {
        val t: Int = foo
    }
    C : E()
}