interface I {
    fun someFoo()
    val someVal: Int
    var someVar: Int
}

class Base1 {
    protected open fun someBar(){}
    open val fromBase: String = ""
}

open class Base2 : Base1() {
}

class A(some<caret>) : Base2(), I

// EXIST: { itemText: "override val someVal: Int", tailText: null, typeText: "I", attributes: "bold" }
// EXIST: { itemText: "override var someVar: Int", tailText: null, typeText: "I", attributes: "bold" }
// NOTHING_ELSE
