trait A {
    val v: Int
}

class AImpl : A {
    override val v: Int = 5
}

public fun box() : String {
    (AImpl() : A).v
    return "OK"
}
