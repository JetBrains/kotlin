fun Thread.foo() {
    <caret>
}

// EXIST_JAVA_ONLY: { lookupString: "priority", itemText: "priority", tailText: " for Thread", typeText: "Int" }
// ABSENT: getPriority
// ABSENT: setPriority
