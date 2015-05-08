// "Add constructor parameters from Base(Int, Int, Any, String, String)" "true"
// ERROR: Redeclaration: p4
// ERROR: Redeclaration: p4
// ERROR: Redeclaration: p4
// ERROR: Redeclaration: p4
open class Base<T>(p1: Int, private val p2: Int, p3: Any, p4: String, p5: T)

class C(p: Int, p2: Int, p3: String, p4: Any, p5: String) : Base<String><caret>
