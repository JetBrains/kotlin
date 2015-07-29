import lib.JavaClass

fun test() = JavaClass().<caret>

// EXIST: { lookupString: "execute", itemText: "execute", tailText: "(Runnable!)", typeText: "Unit" }
// EXIST: { lookupString: "execute", itemText: "execute", tailText: " {...} ((() -> Unit)!)", typeText: "Unit" }
