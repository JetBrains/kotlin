package package1

class Outer {
    class Nested {
        val O = "O"
        val K = "K"
    }
}

fun box() = package1.Outer.Nested().O + Outer.Nested().K
