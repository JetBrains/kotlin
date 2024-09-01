// FIR_IDENTICAL
class Command() {}

fun parse(cmd: String): Command? { return null  }

fun Any.<!EXTENSION_SHADOWED_BY_MEMBER!>equals<!>(other : Any?) : Boolean = this === other

fun main() {
    val command = parse("")
    if (command == null) 1 // error on this line, but must be OK
}
