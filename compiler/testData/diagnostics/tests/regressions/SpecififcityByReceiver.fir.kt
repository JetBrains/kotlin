fun Any.equals(other : Any?) : Boolean = true

fun main() {

    val command : Any = 1

    command?.equals(null)
    command.equals(null)
}
