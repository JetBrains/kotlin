fun foo(): T = <caret>

// EXIST: foo
// EXIST: { lookupString: "object", itemText: "object : T{...}" }
// EXIST: { lookupString: "OO", itemText: "OO", tailText: " (<root>)" }
// NOTHING_ELSE
