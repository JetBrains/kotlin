interface I {
    fun foo()
    val someVal: Int
    var someVar: Int
}

class Base1 {
    protected open fun bar(){}
}

open class Base2 : Base1() {
}

class A : Base2(), I {
    o<caret>
}

// EXIST: { lookupString: "override", itemText: "override" }
// EXIST: { itemText: "override fun bar() {...}", lookupString: "override", tailText: null, typeText: "Base1", attributes: "" }
// EXIST: { itemText: "override operator fun equals(other: Any?): Boolean {...}", lookupString: "override", tailText: null, typeText: "Any", attributes: "" }
// EXIST: { itemText: "override fun foo() {...}", lookupString: "override", tailText: null, typeText: "I", attributes: "bold" }
// EXIST: { itemText: "override fun hashCode(): Int {...}", lookupString: "override", tailText: null, typeText: "Any", attributes: "" }
// EXIST: { itemText: "override val someVal: Int", lookupString: "override", tailText: null, typeText: "I", attributes: "bold" }
// EXIST: { itemText: "override var someVar: Int", lookupString: "override", tailText: null, typeText: "I", attributes: "bold" }
