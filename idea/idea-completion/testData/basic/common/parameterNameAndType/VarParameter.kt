package pack

class FooBar

class Boo

class C(private var b<caret>)

// EXIST: { lookupString: "bar", itemText: "bar: FooBar", tailText: " (pack)" }
// ABSENT: fooBar
// EXIST: { lookupString: "boo", itemText: "boo: Boo", tailText: " (pack)" }
