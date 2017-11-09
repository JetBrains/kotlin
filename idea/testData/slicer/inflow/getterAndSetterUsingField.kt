// FLOW: IN

class A(var b: Boolean) {
    var foo: Int
        get() = if (b) field else 0
        set(value) {
            field = if (b) value else 0
        }

    fun test() {
        val x = <caret>foo
        foo = 1
    }
}