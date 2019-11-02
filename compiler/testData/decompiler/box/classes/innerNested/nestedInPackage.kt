package Package

class Outer {
    class Nested {
        val O = "O"
        val K = "K"
    }
}

fun box() = Package.Outer.Nested().O + Outer.Nested().K
