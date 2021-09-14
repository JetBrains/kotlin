class Base(
    val p1: Int
)

class Sub(
    p: Int
) : Base(p), Unresolved {
    constructor(i : Int, j : Int) : <expr>this(i, j, i * j)</expr>
}
