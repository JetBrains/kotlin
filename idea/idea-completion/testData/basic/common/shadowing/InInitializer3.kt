fun foo(xxx: Int) {
    val xxx: Any = run {
        xx<caret>
    }
}

// EXIST: { lookupString: "xxx", itemText: "xxx", typeText: "Int" }
// NOTHING_ELSE
