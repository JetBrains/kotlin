fun <T> id(t: T) = t


fun main() {
    val a = id("string")
    val b = id(null)
    val c = id(id(a))
}