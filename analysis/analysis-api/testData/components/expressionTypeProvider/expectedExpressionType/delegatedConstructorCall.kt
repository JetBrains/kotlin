class Base(val p1: Int, val p2: Int)

class Derived(override val p1: Int, override val p2: Int) : Base(p1, p2) {
    constructor(s: String) : this(s.length, a<caret>v)
}