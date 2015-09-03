class KotlinClass {
    class Nested
}

fun foo(o: Any) {
    f(o as <caret>)
}

fun f(p: JavaClass.Nested){}
fun f(p: KotlinClass.Nested){}

// EXIST: { lookupString: "Nested", allLookupStrings: "JavaClass, Nested", itemText: "JavaClass.Nested", tailText: " (<root>)" }
// EXIST: { lookupString: "Nested", allLookupStrings: "KotlinClass, Nested", itemText: "KotlinClass.Nested", tailText: " (<root>)" }
