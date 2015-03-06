class Outer {
    class Nested {
        default object {
            val O = "O"
            val K = "K"
        }
    }
    
    fun O() = Nested.O
}

fun box() = Outer().O() + Outer.Nested.K
