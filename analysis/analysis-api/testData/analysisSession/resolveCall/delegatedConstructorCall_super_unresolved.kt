class Base(
    val p1: Int
)

class Sub(
    p: Int
) : Base(p), Unresolved {
    constructor(s: String) : <expr>super(s)</expr>
}
