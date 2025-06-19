class Outer() {
    object { // <no name provided>
        class Inner() {}

        fun getInner() = <expr>Inner()</expr>
    }
}

fun main(args: Array<String>) {
    val inner = Outer.getInner()
}