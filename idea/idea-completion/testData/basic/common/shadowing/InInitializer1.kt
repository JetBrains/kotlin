fun foo(xxx: String) {
    var xxx: Int = xx<caret>
}

// EXIST: { lookupString: "xxx", itemText: "xxx", typeText: "String" }
// NOTHING_ELSE
