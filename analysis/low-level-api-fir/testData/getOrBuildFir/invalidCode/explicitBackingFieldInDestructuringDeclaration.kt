// LANGUAGE: +ExplicitBackingFields

val (a, b) = Tuple()
    field: Int = <expr>1</expr>

class Tuple {
    operator fun component1(): Int = 1
    operator fun component2(): Int = 2
}
