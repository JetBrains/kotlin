interface Interface

operator fun Interface.invoke(): String = "OK"

class Class : Interface

object Holder {
    val value = Class()
}

fun box(): String =
    Holder?.value()!!
