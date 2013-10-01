val a: String = ""
val b: String? = ""

class MyClass {
    val x: String = ""
    val y: String? = ""

    fun foo() {
        val s: String = ""
        val t: String? = ""

        if (b != null) {
            <caret>
        }
    }
}