// "Add constructor parameters from Base(Int)" "true"
// ACTION: Add constructor parameters from Base(Char, Char, String, Int, Any?, String, Boolean,...)
// ACTION: Add constructor parameters from Base(Char, Char, String, Int, Any?, String)

open class Base {
    constructor(p: Int){}
    constructor(p1: Char, p2: Char, p3: String, p4: Int, p5: Any?, p6: String, b: Boolean, c: Char){}
    constructor(p1: Char, p2: Char, p3: String, p4: Int, _p5: Any?, p6: String){}
}

class C : Base<caret>
