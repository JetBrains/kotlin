fun Thread.foo(urlConnection: java.net.URLConnection) {
    with (urlConnection) {
        <caret>
    }
}

// EXIST: { lookupString: "priority", itemText: "priority", tailText: " (from getPriority()/setPriority())", typeText: "Int" }
// EXIST: { lookupString: "isDaemon", itemText: "isDaemon", tailText: " (from isDaemon()/setDaemon())", typeText: "Boolean" }
// EXIST: { lookupString: "url", itemText: "url", tailText: " (from getURL())", typeText: "URL!" }
// ABSENT: getPriority
// ABSENT: setPriority
// ABSENT: { itemText: "isDaemon", tailText: "()" }
// ABSENT: setDaemon
// ABSENT: getURL
