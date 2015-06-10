class C {
    fun xxx(){}

    fun f() {
        xx<caret>
    }
}

// INVOCATION_COUNT: 2
// EXIST: { lookupString: "xxx", itemText: "xxx", tailText: "()", typeText: "Unit" }
// NOTHING_ELSE
