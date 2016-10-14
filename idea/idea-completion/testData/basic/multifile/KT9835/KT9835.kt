interface I

fun foo(r: R<out I>) {
    r.<caret>
}

// EXIST: { itemText: "foo", tailText: " (from getFoo())", typeText: "Int", attributes: "bold" }
// EXIST: { itemText: "f", tailText: "()", typeText: "Int", attributes: "bold" }