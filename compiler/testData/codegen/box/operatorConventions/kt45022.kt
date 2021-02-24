fun box(): String {
    X += 1
    return "OK"
}

object X {
    operator fun plusAssign(any: Any) = Unit
}
