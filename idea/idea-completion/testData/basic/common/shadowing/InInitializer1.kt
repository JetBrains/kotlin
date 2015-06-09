fun foo(xxx: String) {
    var xxx = xx<caret>
}

// EXIST: { lookupString: "xxx", itemText: "xxx", typeText: "String" }
// NOTHING_ELSE
