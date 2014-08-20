class Command() {}

fun parse(<!UNUSED_PARAMETER!>cmd<!>: String): Command? { return null  }

fun Any.equals(other : Any?) : Boolean = this === other

fun main(args: Array<String>) {
    val command = parse("")
    if (command == null) <!UNUSED_EXPRESSION!>1<!> // error on this line, but must be OK
}