fun foo(thread: Thread) {
    thread.<caret>
}

// INVOCATION_COUNT: 2
// EXIST_JAVA_ONLY: { lookupString: "priority", itemText: "priority", tailText: " for Thread", typeText: "Int" }
// EXIST_JAVA_ONLY: getPriority
// EXIST_JAVA_ONLY: setPriority
