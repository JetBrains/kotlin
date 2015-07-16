import java.util

class A {
    <caret>
}

// EXIST: { lookupString: "kotlin", itemText: "kotlin", tailText: null, typeText: null }
// EXIST_JAVA_ONLY: { lookupString: "util", itemText: "util", tailText: " (java.util)", typeText: null }
