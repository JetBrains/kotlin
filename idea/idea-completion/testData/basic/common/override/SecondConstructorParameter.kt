interface Foo {
    val foo: Int
    val bar: Int
}

class A(override val bar: Int, overr<caret>): Foo

// EXIST: { lookupString: "override", itemText: "override" }
// EXIST: { itemText: "override val foo: Int", tailText: null, typeText: "Foo", attributes: "bold" }
// EXIST_JAVA_ONLY: { itemText: "override: Override", tailText: " (java.lang)" }
// NOTHING_ELSE
