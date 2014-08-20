class Command() {}

fun parse(<warning>cmd</warning>: String): Command? { return null  }

fun Any.equals(other : Any?) : Boolean = this === other

fun main(args: Array<String>) {
    val command = parse("")
    if (command == null) <warning>1</warning> // error on this line, but must be OK
}
