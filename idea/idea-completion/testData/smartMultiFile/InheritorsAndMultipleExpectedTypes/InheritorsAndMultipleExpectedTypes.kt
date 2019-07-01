fun bar() {
    foo(<caret>)
}

// EXIST: { lookupString: "object", itemText: "object : T1{...}" }
// EXIST: { lookupString: "object", itemText: "object : T2<X>{...}" }
// EXIST: { lookupString: "object", itemText: "object : T3<Y>{...}" }
// EXIST: { lookupString: "C1", itemText: "C1" }
// EXIST: { lookupString: "C2", itemText: "C2" }
// EXIST: { lookupString: "C3", itemText: "C3" }
// NOTHING_ELSE
