open class B {
    open fun foo() {}
    open fun bar() {}
}

class C : B() {
    override fun foo() {
        super.<caret>
    }
}

// EXIST: { lookupString: "foo", itemText: "foo", tailText: "()", typeText: "Unit", attributes: "bold" }
// EXIST: { lookupString: "bar", itemText: "bar", tailText: "()", typeText: "Unit", attributes: "bold" }
// EXIST: equals
// EXIST: hashCode
// EXIST: toString
// NOTHING_ELSE
