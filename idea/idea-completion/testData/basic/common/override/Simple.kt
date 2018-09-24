interface I {
    infix fun foo(p: Int)
    val someVal: Int
    var someVar: Int
}

class Base1 {
    protected open suspend fun bar(){}
}

open class Base2 : Base1() {
}

class A : Base2(), I {
    o<caret>
}

// EXIST: { lookupString: "override", itemText: "override" }
// EXIST: { itemText: "override suspend fun bar() {...}", lookupString: "override", allLookupStrings: "bar, override", tailText: null, typeText: "Base1", attributes: "" }
// EXIST: { itemText: "override fun equals(other: Any?): Boolean {...}", lookupString: "override", allLookupStrings: "equals, override", tailText: null, typeText: "Any", attributes: "" }
// EXIST: { itemText: "override fun foo(p: Int) {...}", lookupString: "override", allLookupStrings: "foo, override", tailText: null, typeText: "I", attributes: "bold" }
// EXIST: { itemText: "override fun hashCode(): Int {...}", lookupString: "override", allLookupStrings: "hashCode, override", tailText: null, typeText: "Any", attributes: "" }
// EXIST: { itemText: "override val someVal: Int", lookupString: "override", allLookupStrings: "override, someVal", tailText: null, typeText: "I", attributes: "bold" }
// EXIST: { itemText: "override var someVar: Int", lookupString: "override", allLookupStrings: "override, someVar", tailText: null, typeText: "I", attributes: "bold" }
