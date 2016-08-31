package test

typealias MySameFileAlias = (String) -> Int
private typealias MyPrivateAlias = (String, Char) -> Unit

val test: My<caret>

// EXIST: { lookupString: "MySameFileAlias", itemText: "MySameFileAlias", tailText: null, typeText: "public typealias MySameFileAlias = (String) -> Int defined in test", attributes: "" }
// EXIST: { lookupString: "MyPrivateAlias", itemText: "MyPrivateAlias", tailText: null, typeText: "private typealias MyPrivateAlias = (String, Char) -> Unit defined in test", attributes: "" }
// EXIST: { lookupString: "MyAlias", itemText: "MyAlias", tailText: null, typeText: "public typealias MyAlias = MyClass defined in dependency", attributes: "" }
