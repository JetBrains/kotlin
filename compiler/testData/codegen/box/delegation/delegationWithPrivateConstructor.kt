// IGNORE_BACKEND_FIR: JVM_IR
class MyObject private constructor(val delegate: Interface) : Interface by delegate {
    constructor() : this(Delegate())
}

class Delegate : Interface {
    override fun greet(): String {
        return "OK"
    }
}

private interface Interface {
    fun greet(): String
}

fun box(): String {
    return MyObject().greet()
}