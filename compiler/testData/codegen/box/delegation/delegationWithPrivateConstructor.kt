class MyObject private constructor(private val delegate: Interface) : Interface by delegate {
    constructor() : this(Delegate())
}

class Delegate : Interface {
    override fun greet(): String {
        return "OK"
    }
}

interface Interface {
    fun greet(): String
}

fun box(): String {
    return MyObject().greet()
}
