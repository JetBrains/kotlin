open class Base {
    companion object {
        val some = 0
    }
}

class Outer {
    val codegen = ""

    inner class Inner : Base() {
        val c = codegen
    }
}