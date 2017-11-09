fun Any.<warning>equals</warning>(<warning>other</warning> : Any?) : Boolean = true

fun main(args: Array<String>) {

    val command : Any = 1

    command<warning>?.</warning>equals(null)
    command.equals(null)
}