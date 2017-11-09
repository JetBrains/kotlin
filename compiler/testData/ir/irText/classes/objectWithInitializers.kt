abstract class Base

object Test : Base() {
    val x = 1
    val y: Int
    init {
        y = x
    }
}