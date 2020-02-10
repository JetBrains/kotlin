// PROBLEM: none
class Some {
    fun bar() {
        writer().sayHello()
    }

    companion object {
        private fun writer() = object {
            fun <caret>sayHello() {
            }
        }
    }
}