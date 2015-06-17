package pack

class FooBar

class Boo

fun f(b<caret>)

// EXIST: { lookupString: "bar", itemText: "bar: FooBar", tailText: " (pack)" }
// EXIST: { lookupString: "fooBar", itemText: "fooBar: FooBar", tailText: " (pack)" }
// EXIST: { lookupString: "boo", itemText: "boo: Boo", tailText: " (pack)" }
