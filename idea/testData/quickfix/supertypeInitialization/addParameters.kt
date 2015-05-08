// "Add constructor parameters and use them" "true"
open class Base(p1: Int, val p2: Int)

class C(p: Int) : Base<caret>
