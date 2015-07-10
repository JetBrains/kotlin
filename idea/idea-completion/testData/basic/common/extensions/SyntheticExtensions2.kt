fun Thread.foo() {
    <caret>
}

// EXIST_JAVA_ONLY: { lookupString: "priority", itemText: "priority", tailText: " for Thread", typeText: "Int" }
// EXIST_JAVA_ONLY: { lookupString: "daemon", itemText: "daemon", tailText: " for Thread", typeText: "Boolean" }
// ABSENT: getPriority
// ABSENT: setPriority
// ABSENT: isDaemon
// ABSENT: setDaemon
