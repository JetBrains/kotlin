data class A(val x: Unit)

fun box(): String {
    val a = A(#())
    return if ("$a" == "A{x=()}") "OK" else "$a"
}
