package testing

private fun times<T>(times : Int, body : () -> T) {}

fun main(args: Array<String>) {
    times(3) {
        <caret>
    }
}