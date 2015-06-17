import java.io.*

fun f(printSt<caret>)

// EXIST_JAVA_ONLY: { lookupString: "printStream", itemText: "printStream: PrintStream", tailText: " (java.io)" }
// NUMBER_JAVA: 1
