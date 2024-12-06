@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class Marker

class My(@Marker val x: String)

fun main() {
    val my = My("")
    my.x
}
