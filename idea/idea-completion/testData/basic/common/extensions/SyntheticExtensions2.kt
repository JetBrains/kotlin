fun Thread.foo(urlConnection: java.net.URLConnection) {
    with (urlConnection) {
        <caret>
    }
}

// EXIST_JAVA_ONLY: { lookupString: "priority", itemText: "priority", tailText: " for Thread", typeText: "Int" }
// EXIST_JAVA_ONLY: { lookupString: "daemon", itemText: "daemon", tailText: " for Thread", typeText: "Boolean" }
// EXIST_JAVA_ONLY: { lookupString: "URL", itemText: "URL", tailText: " for URLConnection", typeText: "URL!" }
// ABSENT: getPriority
// ABSENT: setPriority
// ABSENT: isDaemon
// ABSENT: setDaemon
// ABSENT: getURL
