// "Create property 'foo'" "true"
// ERROR: Property must be initialized or be abstract

class Test {
    val foo1 = 1

    fun test(): Int {
        return <caret>foo
    }
}

val bar = 1
