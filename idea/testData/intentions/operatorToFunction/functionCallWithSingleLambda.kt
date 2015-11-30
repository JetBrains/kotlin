class Mocha() {
    operator fun invoke(f: (Int) -> String) {}
}
fun main() {
    val mocha = Mocha()
    val testing = mocha<caret>{ x: Int -> "hello world" }
}