// !CHECK_TYPE
fun <T> id(t: T) = t

fun foo() {
    val i = id { 22 } //type inference error: no information for parameter
    i checkType { it : _<()->Int> }
}
