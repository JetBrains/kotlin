package ppp

import java.io.*

class MyPrintStream

fun f1(printStream: PrintStream){}
fun f2(printStream: PrintStream?){}
fun f3(printStream: MyPrintStream){}

fun f(printStr<caret>)

// EXIST_JAVA_ONLY: { lookupString: "printStream", itemText: "printStream: PrintStream", tailText: " (java.io)" }
// EXIST_JAVA_ONLY: { lookupString: "printStream", itemText: "printStream: PrintStream?", tailText: " (java.io)" }
// EXIST: { lookupString: "printStream", itemText: "printStream: MyPrintStream", tailText: " (ppp)" }
// EXIST: { lookupString: "myPrintStream", itemText: "myPrintStream: MyPrintStream", tailText: " (ppp)" }
// NOTHING_ELSE
