package testing.rename

interface AP {
    var second: Int // <--- Rename base here, rename as Java getter and setter here
}

public open class BP: AP {
    override var second = 1 // <--- Rename overriden here
}

class CP: BP() {
    override var second = 2
}

class CPOther {
    var first: Int = 111
}

fun usagesProp() {
    val b = BP()
    val a: AP = b
    val c = CP()

    a.second
    b.second
    c.second

    a.second = 1
    b.second = 2
    c.second = 3

    CPOther().first
}