sealed class X(val name: String ="X")

class Y: X("Y")

class Z: X("Z")

fun last(): X = Z()

