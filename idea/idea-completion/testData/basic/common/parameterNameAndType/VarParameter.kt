package pack

class FooBar

class C(private var b<caret>) {
    class Boo
}

// EXIST: { lookupString: "bar", itemText: "bar: FooBar", tailText: " (pack)" }
// EXIST: { lookupString: "fooBar", itemText: "fooBar: FooBar", tailText: " (pack)" }
// EXIST: { lookupString: "boo", itemText: "boo: Boo", tailText: " (pack.C)" }
