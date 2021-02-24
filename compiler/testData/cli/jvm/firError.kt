fun println(val x: Int) {}

fun main() {
    val x: Int
    println(x)
}

private class Private

class Public : Private() {
    val x: Private
}