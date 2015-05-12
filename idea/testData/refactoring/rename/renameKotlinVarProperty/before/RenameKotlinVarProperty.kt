package testing.rename

interface AP {
    var first: Int // <--- Rename base here, rename as Java getter and setter here
}

public open class BP: AP {
    override var first = 1 // <--- Rename overriden here
}

class CP: BP() {
    override var first = 2
}

class CPOther {
    var first: Int = 111
}

fun usagesProp() {
    val b = BP()
    val a: AP = b
    val c = CP()

    a.first
    b.first
    c.first

    a.first = 1
    b.first = 2
    c.first = 3

    CPOther().first
}