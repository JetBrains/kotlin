data class A(val x: Unit)

fun box(): String {
    val a = A(Unit.VALUE)
    return if ("$a" == "A(x=Unit.VALUE)") "OK" else "$a"
}
