package testing

private fun times<T>(times : Int, body : () -> T) {
    for (var i in 1..times) {
        body()
    }
}

fun main(args: Array<String>) {
    times(3) {
        <caret>
    }
}
