// IGNORE_BACKEND_FIR: JVM_IR
interface Intf {
    val aValue: String
}

class ClassB {
    val x = { "OK" }

    val value: Intf = object : Intf {
        override val aValue = x()
    }
}

fun box() : String {
    return ClassB().value.aValue
}