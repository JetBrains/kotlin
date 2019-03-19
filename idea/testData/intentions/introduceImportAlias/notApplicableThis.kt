// IS_APPLICABLE: false
class Test {
    fun check() {}
    fun test2() {
        <caret>this.check()
    }
}