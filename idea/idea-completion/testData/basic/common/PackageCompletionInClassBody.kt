import java.util

class A {
    <caret>
}

// EXIST: { lookupString: "kotlin", itemText: "kotlin", tailText: null, typeText: null }
// EXIST: { lookupString: "util", itemText: "util", tailText: " (java.util)", typeText: null }
