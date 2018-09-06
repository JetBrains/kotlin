package parameterless

class A {
    companion object {
        @JvmStatic
        fun main() { // no
        }
    }
}

object B {
    @JvmStatic
    fun main() { // no
    }
}
