fun Any.<!EXTENSION_SHADOWED_BY_MEMBER!>equals<!>(other : Any?) : Boolean = true

fun main() {

    val command : Any = 1

    <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>command<!UNNECESSARY_SAFE_CALL!>?.<!>equals(null)<!>
    command.equals(null)
}
