// FLOW: IN

class A(var b: Boolean) {
    var foo: Int = 0
        get() = 1
        set(value) {
            field = value + 1
        }

    fun test() {
        val x = <caret>foo
    }
}