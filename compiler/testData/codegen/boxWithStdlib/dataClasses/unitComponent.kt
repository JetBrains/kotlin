data class A(val x: Unit)

fun box(): String {
    val a = A(Unit.VALUE)
    return if (a.component1() is Unit) "OK" else "Fail ${a.component1()}"
}
