interface I {
    fun foo()
    val someVal: Int
    var someVar: Int
}

class Base1 {
    protected open fun bar(){}
    open val fromBase: String = ""
}

open class Base2 : Base1() {
}

class A(overr<caret>) : Base2(), I

// EXIST: { lookupString: "override", itemText: "override" }
// EXIST: { itemText: "override val someVal: Int", tailText: null, typeText: "I" }
// EXIST: { itemText: "override var someVar: Int", tailText: null, typeText: "I" }
// EXIST: { itemText: "override val fromBase: String", tailText: null, typeText: "Base1" }
// EXIST_JAVA_ONLY: { lookupString: "override", itemText: "override: Override" }
// NOTHING_ELSE
