// "Add constructor parameters from Base(p1: Int, p2: Int)" "true"
open class Base(p1: Int, val p2: Int)

class C(p: Int) : Base<caret>
