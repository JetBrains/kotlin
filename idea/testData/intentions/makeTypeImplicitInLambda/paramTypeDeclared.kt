fun main() {
    val foo: (Int) -> String = {x: Int<caret> -> "This number is " + x}
}
