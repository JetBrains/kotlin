// IGNORE_BACKEND_FIR: JVM_IR
class Outer {
    class Nested {
        companion object {
            val O = "O"
            val K = "K"
        }
    }
    
    fun O() = Nested.O
}

fun box() = Outer().O() + Outer.Nested.K
