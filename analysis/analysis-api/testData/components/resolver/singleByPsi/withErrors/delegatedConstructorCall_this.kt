class Base(
    val p1: Int
)

class Sub(
    override val p1: Int
) : Base(p1) {
    constructor(s: String) : <expr>this(s.length)</expr>
}
