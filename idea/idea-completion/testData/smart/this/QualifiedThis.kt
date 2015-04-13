class Outer {
    inner class Inner {
        fun String.foo() {
            val v: Any = this@<caret>
        }
    }
}

// ABSENT: this
// EXIST: { lookupString: "this@foo", itemText: "this", tailText: "@foo", typeText: "String", attributes: "bold" }
// EXIST: { lookupString: "this@Inner", itemText: "this", tailText: "@Inner", typeText: "Outer.Inner", attributes: "bold" }
// EXIST: { lookupString: "this@Outer", itemText: "this", tailText: "@Outer", typeText: "Outer", attributes: "bold" }
// NUMBER: 3
