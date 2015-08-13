fun foo(javaClass: JavaClass<String, Int>) {
    javaClass.<caret>
}

// EXIST: { lookupString: "something", itemText: "something", tailText: " (from getSomething()/setSomething())", typeText: "Int!", attributes: "bold" }
