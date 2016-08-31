package test2

fun foo(ali<caret>)

// EXIST: { lookupString: "alias : MyAlias", itemText: "alias: MyAlias", typeText: "public typealias MyAlias = MyClass defined in test", attributes: "" }
