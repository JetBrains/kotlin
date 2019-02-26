// "Create property 'foo'" "true"
// ERROR: Property must be initialized or be abstract

class Test {
    fun test(): Int {
        return <caret>foo
    }

    val foo1 = 1
}

val bar = 1
