package pack

class FooBar

class Boo

fun f(b<caret>)

// EXIST: { lookupString: "bar : FooBar", itemText: "bar: FooBar", tailText: " (pack)" }
// ABSENT: { itemText: "fooBar: FooBar" }
// EXIST: { lookupString: "boo : Boo", itemText: "boo: Boo", tailText: " (pack)" }
