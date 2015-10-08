package pack

class FooBar

class Boo

class C(private var b<caret>)

// EXIST: { lookupString: "bar: FooBar", itemText: "bar: FooBar", tailText: " (pack)" }
// ABSENT: fooBar: FooBar
// EXIST: { lookupString: "boo: Boo", itemText: "boo: Boo", tailText: " (pack)" }
