fun foo(javaClass: JavaClass<String>) {
    javaClass.<caret>
}

// EXIST: { lookupString: "something", itemText: "something", tailText: " for JavaClass<String>", typeText: "String!" }
