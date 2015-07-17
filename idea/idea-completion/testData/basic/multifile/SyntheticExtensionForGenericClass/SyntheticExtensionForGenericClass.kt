fun foo(javaClass: JavaClass<String>) {
    javaClass.<caret>
}

// EXIST: { lookupString: "something", itemText: "something", tailText: " (from getSomething()/setSomething())", typeText: "String!" }
