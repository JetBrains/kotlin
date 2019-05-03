// IS_APPLICABLE: false
open class SuperClass {
    fun check() {}
}

class Test : SuperClass() {
    fun test2() {
        <caret>super.check()
    }
}