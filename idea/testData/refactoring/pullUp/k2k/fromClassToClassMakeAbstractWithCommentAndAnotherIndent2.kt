open class A

class Root {
    class <caret>B : A() {
        // INFO: {"checked": "true"}
        fun barw() {
        }

        // INFO: {"checked": "true", "toAbstract": "true"}
        val foo8: Int = 1
    }
}