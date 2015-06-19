package pack

class FooBar

class Boo

fun f(myB<caret>)

// EXIST: { lookupString: "Bar", itemText: "myBar: FooBar", tailText: " (pack)" }
// ABSENT: FooBar
// EXIST: { lookupString: "Boo", itemText: "myBoo: Boo", tailText: " (pack)" }
