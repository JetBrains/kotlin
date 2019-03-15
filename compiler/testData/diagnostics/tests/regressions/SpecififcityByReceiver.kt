fun Any.<!EXTENSION_SHADOWED_BY_MEMBER!>equals<!>(<!UNUSED_PARAMETER!>other<!> : Any?) : Boolean = true

fun main() {

    val command : Any = 1

    command<!UNNECESSARY_SAFE_CALL!>?.<!>equals(null)
    command.equals(null)
}
