// "Add missing 'constructor' keyword in whole project" "true"

annotation class Ann(val x: Int = 1)

class A @Ann(1)<caret>private (x: Int) {
    inner class B() // do not insert here
    inner class C        protected                () {
        fun foo() {
            @data class Local private()
        }
    }
}
