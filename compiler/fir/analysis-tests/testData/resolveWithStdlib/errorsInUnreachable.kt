fun case(): String {
    val x0 = false
    val x1: String
    val x: Boolean
    try {
        <!VAL_REASSIGNMENT!>x0<!> = (throw Exception()) || true  //VAL_REASSIGNMENT should be
        !<!UNINITIALIZED_VARIABLE!>x<!> //ok, unreachable code   UNINITIALIZED_VARIABLE should be
        val a: Int = <!UNINITIALIZED_VARIABLE!>x1<!>.toInt() //ok, unreachable code UNINITIALIZED_VARIABLE should be
    } catch (e: Exception) {
        return "OK"
    }
    return "NOK"
}
