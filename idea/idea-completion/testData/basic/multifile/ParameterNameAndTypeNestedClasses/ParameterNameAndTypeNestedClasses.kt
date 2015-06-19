fun f(nested: Outer.Nested?){}
fun f(nested: Outer.Nested.NestedNested?){}

fun foo(nest<caret>)

// EXIST: { lookupString: "nested", itemText: "nested: Outer.Nested?", tailText: " (<root>)" }
// EXIST: { lookupString: "nested", itemText: "nested: Outer.Nested", tailText: " (<root>)" }
// EXIST: { lookupString: "nested", itemText: "nested: Outer.Nested.NestedNested?", tailText: " (<root>)" }
// EXIST: { lookupString: "nested", itemText: "nested: Outer.Nested.NestedNested", tailText: " (<root>)" }
// EXIST: { lookupString: "nested", itemText: "nested: JavaOuter.Nested", tailText: " (<root>)" }
// EXIST: { lookupString: "nested", itemText: "nested: JavaOuter.Nested.NestedNested", tailText: " (<root>)" }
